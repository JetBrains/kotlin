/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.IntType
import org.jetbrains.kotlin.arguments.dsl.types.PathType
import org.jetbrains.kotlin.cli.arguments.generator.levelToClassNameMap
import org.jetbrains.kotlin.generators.kotlinpoet.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass

internal class BtaImplGenerator(
    private val targetPackage: String,
    private val skipXX: Boolean,
    private val kotlinVersion: KotlinReleaseVersion,
    private val generateCompatLayer: Boolean,
) : BtaGenerator {

    private val outputs = mutableListOf<Pair<Path, String>>()

    override fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: ClassName?): GeneratorOutputs {
        val apiClassName = level.name.capitalizeAsciiOnly()
        val implClassName = apiClassName + "Impl"
        val mainFileAppendable = createGeneratedFileAppendable()
        val mainFile = FileSpec.builder(targetPackage, implClassName).apply {
            // Kotlinpoet requires these aliased imports when there's a name clash in the current context or else it calls the wrong member
            addAliasedImport(MemberName("org.jetbrains.kotlin.compilerRunner", "toArgumentStrings"), "compilerToArgumentStrings")
            addAliasedImport(MemberName(ClassName("org.jetbrains.kotlin.config", "KotlinCompilerVersion"), "VERSION"), "KC_VERSION")

            addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .addMember("%T::class", ANNOTATION_EXPERIMENTAL).build()
            )
            classType(implClassName) {
                addModifiers(KModifier.INTERNAL)
                if (!level.isLeaf()) {
                    addModifiers(KModifier.ABSTRACT)
                }
                if (parentClass != null) {
                    superclass(parentClass)
                } else {
                    property(
                        "internalArguments",
                        ClassName("kotlin.collections", "MutableSet").parameterizedBy(typeNameOf<String>()),
                        KModifier.PROTECTED
                    ) {
                        initializer("%M()", MemberName("kotlin.collections", "mutableSetOf"))
                    }
                }

                addSuperinterface(ClassName(API_ARGUMENTS_PACKAGE, level.name.capitalizeAsciiOnly()))
                addSuperinterface(ClassName(API_ARGUMENTS_PACKAGE, level.name.capitalizeAsciiOnly()).nestedClass("Builder"))

                val toCompilerConverterFun = toCompilerConverterFunBuilder(level, parentClass)
                val applyCompilerArgumentsFun = applyCompilerArgumentsFunBuilder(level, parentClass)
                val defaultsInitializer = CodeBlock.builder()

                val argumentTypeNameString =
                    generateArgumentType(apiClassName, includeSinceVersion = false, registerAsKnownArgument = true)
                val argumentTypeName = ClassName(API_ARGUMENTS_PACKAGE, apiClassName, argumentTypeNameString)
                val argumentImplTypeName = ClassName(targetPackage, implClassName, argumentTypeNameString)

                generateGetPutFunctions(argumentTypeName, argumentImplTypeName)

                addType(TypeSpec.companionObjectBuilder().apply {
                    property(
                        "knownArguments",
                        ClassName("kotlin.collections", "MutableSet").parameterizedBy(ClassName("kotlin", "String")),
                        KModifier.PRIVATE
                    ) {
                        initializer("%M()", MemberName("kotlin.collections", "mutableSetOf"))
                    }
                    generateOptions(
                        arguments = level.transformImplArguments(),
                        implClassName = implClassName,
                        argumentTypeName = argumentImplTypeName,
                        applyCompilerArgumentsFun = applyCompilerArgumentsFun,
                        toCompilerConverterFun = toCompilerConverterFun,
                        defaultsInitializer = defaultsInitializer,
                    )
                }.build())

                // Initialize default values for custom arguments
                defaultsInitializer.build().takeIf { it.isNotEmpty() }?.let { addInitializerBlock(it) }

                if (level.isLeaf()) {
                    function("deepCopy") {
                        addModifiers(KModifier.OVERRIDE)
                        returns(ClassName(targetPackage, implClassName))
                        addStatement(
                            "return %T().also { newArgs -> newArgs.applyArgumentStrings(toArgumentStrings()) }",
                            ClassName(targetPackage, implClassName)
                        )
                    }
                    function("build") {
                        addModifiers(KModifier.OVERRIDE)
                        returns(ClassName(API_ARGUMENTS_PACKAGE, apiClassName))
                        addStatement("return deepCopy()")
                    }
                    addSuperinterface(
                        ClassName(targetPackage.removeSuffix("arguments"), "DeepCopyable").parameterizedBy(
                            ClassName(targetPackage, implClassName)
                        )
                    )
                    toCompilerConverterFun.addStatement(
                        "arguments.internalArguments = %M<%T>(internalArguments.toList()).internalArguments",
                        MemberName("org.jetbrains.kotlin.cli.common.arguments", "parseCommandLineArguments"),
                        level.getCompilerArgumentsClassName()
                    )

                    primaryConstructor(FunSpec.constructorBuilder().apply {
                        addStatement("applyCompilerArguments(%T())", level.getCompilerArgumentsClassName())
                    }.build())
                }
                toCompilerConverterFun.addStatement("return arguments")
                addFunction(toCompilerConverterFun.build())

                applyCompilerArgumentsFun.addStatement("internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })")
                addFunction(applyCompilerArgumentsFun.build())

                maybeAddApplyArgumentStringsFun(level, parentClass)
                maybeAddToArgumentsStringFun(level, parentClass)
            }
        }.build()
        mainFile.writeTo(mainFileAppendable)
        outputs += Path(mainFile.relativePath) to mainFileAppendable.toString()
        return GeneratorOutputs(ClassName(targetPackage, implClassName), outputs)
    }

    private fun TypeSpec.Builder.generateOptions(
        arguments: Collection<BtaCompilerArgument<*>>,
        implClassName: String,
        argumentTypeName: ClassName,
        applyCompilerArgumentsFun: FunSpec.Builder,
        toCompilerConverterFun: FunSpec.Builder,
        defaultsInitializer: CodeBlock.Builder,
    ) {
        arguments.forEach { argument ->
            val name = argument.extractName()
            if (skipXX && name.startsWith("XX_")) return@forEach

            // argument is newer than currently generated version, skip it
            if (argument.introducedSinceVersion > kotlinVersion) {
                return@forEach
            }

            val wasRemoved = argument.removedSinceVersion?.let { removedVersion ->
                // argument was removed in or before current version - 3, skip it entirely
                if (removedVersion <= getOldestSupportedVersion(kotlinVersion)) {
                    return@forEach
                }
                true
            } ?: false

            // argument was introduced in one of recent versions, so it might not exist in older supported version
            val wasIntroducedRecently = (argument.introducedSinceVersion > getOldestSupportedVersion(kotlinVersion))

            // generate impl mirror of arguments
            val argumentTypeParameter = when (argument.valueType) {
                is BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType -> {
                    val type = argument.valueType.kType
                    val classifier = type.classifier as? KClass<*> ?: error("Type is not a KClass: $type")
                    when {
                        classifier.java.isEnum -> {
                            val classifier = type.classifier as KClass<*>
                            classifier.toBtaEnumClassName()
                        }
                        classifier.isCustomType -> {
                            val classifier = type.classifier as KClass<*>
                            classifier.toBtaCustomClassName()
                        }
                        else -> {
                            type.asTypeName()
                        }
                    }
                }
                is BtaCompilerArgumentValueType.CustomArgumentValueType -> argument.valueType.type
            }.copy(nullable = argument.valueType.isNullable)

            property(name, argumentTypeName.parameterizedBy(argumentTypeParameter)) {
                initializer("%T(%S)", argumentTypeName, name)
            }
            when (argument) {
                is BtaCompilerArgument.SSoTCompilerArgument -> {
                    generateAutomaticArgumentsPropagators(
                        implClassName,
                        name,
                        argumentTypeParameter,
                        argument,
                        wasRemoved,
                        argument.effectiveCompilerName,
                        toCompilerConverterFun,
                        wasIntroducedRecently,
                        applyCompilerArgumentsFun,
                        argumentTypeParameter
                    )
                }

                is BtaCompilerArgument.SSoTCompilerArgumentCompat -> {
                    generateCompatArgumentsPropagators(
                        implClassName,
                        name,
                        argumentTypeParameter,
                        argument,
                        wasRemoved,
                        argument.effectiveCompilerName,
                        toCompilerConverterFun,
                        wasIntroducedRecently,
                        applyCompilerArgumentsFun,
                        argumentTypeParameter,
                        argument.applierSimpleName
                    )
                }

                is BtaCompilerArgument.CustomCompilerArgument -> {
                    defaultsInitializer.addStatement("optionsMap[%S] = %L", name, argument.defaultValue)
                    generateCustomRepresentation(
                        implClassName,
                        name,
                        argument,
                        wasRemoved,
                        toCompilerConverterFun,
                        applyCompilerArgumentsFun,
                        wasIntroducedRecently,
                    )
                }
            }
        }
    }

    private fun generateCustomRepresentation(
        implClassName: String,
        name: String,
        argument: BtaCompilerArgument.CustomCompilerArgument,
        wasRemoved: Boolean,
        toCompilerConverterFun: FunSpec.Builder,
        applyCompilerArgumentsFun: FunSpec.Builder,
        wasIntroducedRecently: Boolean,
    ) {
        val member = MemberName(ClassName(targetPackage, implClassName, "Companion"), name)
        val applier = MemberName(targetPackage, argument.applierSimpleName)

        CodeBlock.builder().apply {
            add("if (%M in this) { ", member)
            add("arguments.%M(get(%M))", applier, member)
            add("}")
        }.build().also { setStatement ->
            toCompilerConverterFun.addSafeSetStatement(
                wasIntroducedRecently,
                wasRemoved,
                name,
                argument,
                setStatement,
                generateCompatLayer,
            )
        }

        applyCompilerArgumentsFun.addSafeMethodAccessStatement(CodeBlock.builder().apply {
            add("this[%M] = %M(this[%M], arguments)", member, applier, member)
        }.build(), failOnNoSuchMethod = false)
    }

    /**
     * Generates code that configures for example [org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments] from [org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments] and vice versa
     */
    private fun generateAutomaticArgumentsPropagators(
        implClassName: String,
        name: String,
        type: TypeName,
        argument: BtaCompilerArgument.SSoTCompilerArgument,
        wasRemoved: Boolean,
        effectiveCompilerName: String,
        toCompilerConverterFun: FunSpec.Builder,
        wasIntroducedRecently: Boolean,
        applyCompilerArgumentsFun: FunSpec.Builder,
        argumentTypeParameter: TypeName,
    ) {
        val member = MemberName(ClassName(targetPackage, implClassName, "Companion"), name)

        // BTA → Compiler conversion
        CodeBlock.builder().apply {
            add("if (%M in this) { ", member)
            val valueToAssign = buildBtaToCompilerValueTransform(member, type, argument)
            val assignment = buildCompilerAssignment(effectiveCompilerName, wasRemoved, valueToAssign)
            add("%L", assignment)
            add("}")
        }.build().also { setStatement ->
            toCompilerConverterFun.addSafeSetStatement(
                wasIntroducedRecently,
                wasRemoved,
                name,
                argument,
                setStatement,
                generateCompatLayer,
            )
        }

        // Compiler → BTA conversion
        val compilerToBtaStatement = buildCompilerToBtaValueTransform(
            member, type, argument, effectiveCompilerName, wasRemoved, argumentTypeParameter
        )
        applyCompilerArgumentsFun.addSafeMethodAccessStatement(compilerToBtaStatement, failOnNoSuchMethod = false)
    }

    /**
     * Generates code for compat arguments with ClassCastException handling
     */
    private fun generateCompatArgumentsPropagators(
        implClassName: String,
        name: String,
        type: TypeName,
        argument: BtaCompilerArgument.SSoTCompilerArgumentCompat,
        wasRemoved: Boolean,
        effectiveCompilerName: String,
        toCompilerConverterFun: FunSpec.Builder,
        wasIntroducedRecently: Boolean,
        applyCompilerArgumentsFun: FunSpec.Builder,
        argumentTypeParameter: TypeName,
        applierSimpleName: String,
    ) {
        val member = MemberName(ClassName(targetPackage, implClassName, "Companion"), name)
        val applier = MemberName(targetPackage, applierSimpleName)

        // BTA → Compiler conversion with ClassCastException handling
        CodeBlock.builder().apply {
            add("if (%M in this) { ", member)
            val valueToAssign = buildBtaToCompilerValueTransform(member, type, argument)
            val assignment = buildCompilerAssignment(effectiveCompilerName, wasRemoved, valueToAssign)
            add("try { %L } catch(e: ClassCastException) { arguments.%M(get(%M)) }", assignment, applier, member)
            add("}")
        }.build().also { setStatement ->
            toCompilerConverterFun.addSafeSetStatement(
                wasIntroducedRecently,
                wasRemoved,
                name,
                argument,
                setStatement,
                generateCompatLayer,
            )
        }

        // Compiler → BTA conversion with ClassCastException handling
        val compilerToBtaStatement = buildCompilerToBtaValueTransform(
            member, type, argument, effectiveCompilerName, wasRemoved, argumentTypeParameter
        )
        val wrappedStatement = CodeBlock.of(
            "try { %L } catch (e: ClassCastException) { %M(this[%M], arguments) }",
            compilerToBtaStatement,
            applier,
            member
        )

        applyCompilerArgumentsFun.addSafeMethodAccessStatement(wrappedStatement, failOnNoSuchMethod = false)
    }

    /**
     * Builds the value transformation from BTA to compiler (e.g., enum.stringValue, int.toString(), path.absolutePathStringOrThrow())
     */
    private fun buildBtaToCompilerValueTransform(
        member: MemberName,
        type: TypeName,
        argument: BtaCompilerArgument<BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType>,
    ): CodeBlock = CodeBlock.builder().apply {
        add("get(%M)", member)
        when {
            type.isGeneratedEnum -> {
                add(maybeGetNullabilitySign(argument) + ".stringValue")
            }
            argument.valueType.origin is IntType -> {
                add(maybeGetNullabilitySign(argument) + ".toString()")
            }
            argument.valueType.origin is PathType -> {
                add(
                    maybeGetNullabilitySign(argument) + ".%M()",
                    MemberName(
                        packageName = targetPackage,
                        simpleName = "absolutePathStringOrThrow",
                        isExtension = true
                    )
                )
            }
            type.isCustomType -> {
                add(
                    maybeGetNullabilitySign(argument) + ".%M()",
                    MemberName(
                        packageName = targetPackage,
                        simpleName = "toArgumentString",
                        isExtension = true
                    )
                )
            }
            else -> add("")
        }
    }.build()

    /**
     * Builds the assignment statement: arguments.property = value or arguments.setUsingReflection(...)
     */
    private fun buildCompilerAssignment(
        effectiveCompilerName: String,
        wasRemoved: Boolean,
        valueToAssign: CodeBlock,
    ): CodeBlock = CodeBlock.builder().apply {
        if (wasRemoved) {
            add(
                "arguments.%M(%S, %L)",
                MemberName(targetPackage, "setUsingReflection", isExtension = true),
                effectiveCompilerName,
                valueToAssign
            )
        } else {
            add("arguments.%N = %L", effectiveCompilerName, valueToAssign)
        }
    }.build()

    /**
     * Builds the value transformation from compiler to BTA (e.g., string to enum, string.toInt(), path parsing)
     */
    private fun buildCompilerToBtaValueTransform(
        member: MemberName,
        type: TypeName,
        argument: BtaCompilerArgument<BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType>,
        effectiveCompilerName: String,
        wasRemoved: Boolean,
        argumentTypeParameter: TypeName,
    ): CodeBlock = CodeBlock.builder().apply {
        add("this[%M] = ", member)
        if (wasRemoved) {
            add("arguments.%M(%S)", MemberName(targetPackage, "getUsingReflection", isExtension = true), effectiveCompilerName)
        } else {
            add("arguments.%N", effectiveCompilerName)
        }

        when {
            type.isGeneratedEnum -> {
                add(maybeGetNullabilitySign(argument))
                add(
                    $$".let { %T.entries.firstOrNull { entry -> entry.stringValue == it } ?: throw %M(\"Unknown -$${argument.name} value: $it\") }",
                    argumentTypeParameter.copy(nullable = false),
                    MemberName("org.jetbrains.kotlin.buildtools.api", "CompilerArgumentsParseException"),
                )
            }
            argument.valueType.origin is IntType -> {
                add(maybeGetNullabilitySign(argument))
                add(".let { it.toInt() }")
            }
            argument.valueType.origin is PathType -> {
                add(maybeGetNullabilitySign(argument))
                add(".let { %M(it) }", MemberName(KOTLIN_IO_PATH, "Path"))
            }
            type.isCustomType -> {
                add(
                    maybeGetNullabilitySign(argument) + ".%M()",
                    MemberName(
                        packageName = targetPackage,
                        simpleName = "to${argument.name.replaceFirstChar { it.uppercase() }}",
                        isExtension = true
                    )
                )
            }
            else -> add("")
        }
    }.build()

    fun TypeSpec.Builder.generateGetPutFunctions(parameter: ClassName, implParameter: ClassName) {
        val mapProperty = property(
            "optionsMap",
            ClassName("kotlin.collections", "MutableMap").parameterizedBy(typeNameOf<String>(), ANY.copy(nullable = true))
        ) {
            addModifiers(KModifier.PRIVATE)
            initializer("%M()", MemberName("kotlin.collections", "mutableMapOf"))
        }
        function("get") {
            val typeParameter = TypeVariableName("V")
            annotation<Suppress> {
                addMember("%S", "UNCHECKED_CAST")
            }
            if (targetPackage == IMPL_ARGUMENTS_PACKAGE) {
                annotation(ANNOTATION_USE_FROM_IMPL_RESTRICTED) {}
            }
            returns(typeParameter)
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addStatement($$"check(key.id in optionsMap) { \"Argument ${key.id} is not set and has no default value\" }")
            addStatement("return %N[key.id] as %T", mapProperty, typeParameter)
        }
        function("set") {
            if (targetPackage == IMPL_ARGUMENTS_PACKAGE) {
                annotation(ANNOTATION_USE_FROM_IMPL_RESTRICTED) {}
            }
            val typeParameter = TypeVariableName("V")
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)
            val currentKotlinVersion = if (generateCompatLayer) {
                addStatement(
                    "val currentKotlinVersion = %T(KC_VERSION)",
                    ClassName("org.jetbrains.kotlin.tooling.core", "KotlinToolingVersion")
                )
                CodeBlock.of("(currentKotlinVersion.major, currentKotlinVersion.minor, currentKotlinVersion.patch)")
            } else {
                CodeBlock.of(
                    "(%L, %L, %L)",
                    kotlinVersion.major,
                    kotlinVersion.minor,
                    kotlinVersion.patch
                )
            }
            addCode(
                CodeBlock.builder()
                    .beginControlFlow(
                        "if (key.availableSinceVersion > %T%L)",
                        kotlinVersionType,
                        currentKotlinVersion
                    )
                    .addStatement(
                        $$"throw %T(\"${key.id} is available only since ${key.availableSinceVersion}\")",
                        IllegalStateException::class
                    )
                    .endControlFlow()
                    .build()
            )
            addStatement("%N[key.id] = %N", mapProperty, "value")
        }

        function("contains") {
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            returns(BOOLEAN)
            addParameter("key", parameter.parameterizedBy(STAR))
            addStatement("return key.id in optionsMap")
        }

        function("get") {
            val typeParameter = TypeVariableName("V")
            annotation<Suppress> {
                addMember("%S", "UNCHECKED_CAST")
            }
            returns(typeParameter)
            addModifiers(KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", implParameter.parameterizedBy(typeParameter))
            addStatement("return %N[key.id] as %T", mapProperty, typeParameter)
        }
        function("set") {
            val typeParameter = TypeVariableName("V")
            addModifiers(KModifier.OPERATOR, KModifier.PRIVATE)
            addTypeVariable(typeParameter)
            addParameter("key", implParameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)
            addStatement("%N[key.id] = %N", mapProperty, "value")
        }

        function("contains") {
            addModifiers(KModifier.OPERATOR)
            returns(BOOLEAN)
            addParameter("key", implParameter.parameterizedBy(STAR))
            addStatement("return key.id in optionsMap")
        }
    }
}

internal fun FunSpec.Builder.addSafeSetStatement(
    wasIntroducedRecently: Boolean,
    wasRemoved: Boolean,
    name: String,
    argument: BtaCompilerArgument<*>,
    setStatement: CodeBlock,
    generateCompatLayer: Boolean,
) {
    // There's no need in future compatibility check for non-compat layer.
    // The main impl is tied to a compiler version and could not know about the future changes.
    if (wasRemoved || generateCompatLayer && wasIntroducedRecently) {
        val errorMessage = CodeBlock.of(
            "%P",
            buildString {
                append($$"Compiler parameter not recognized: $$name. Current compiler version is: $KC_VERSION, but")
                if (wasIntroducedRecently) {
                    append(" the argument was introduced in ${argument.introducedSinceVersion.releaseName}")
                }
                if (wasRemoved) {
                    append(if (wasIntroducedRecently) " and" else " the argument was")
                    append(" removed in ${argument.removedSinceVersion?.releaseName}")
                }
            }
        )
        addSafeMethodAccessStatement(setStatement, failOnNoSuchMethod = true, errorMessage = errorMessage)
    } else {
        addStatement("%L", setStatement)
    }
}

private fun maybeGetNullabilitySign(argument: BtaCompilerArgument<*>): String = (if (argument.valueType.isNullable) "?" else "")

private fun TypeSpec.Builder.maybeAddToArgumentsStringFun(level: KotlinCompilerArgumentsLevel, parentClass: TypeName?) {
    if (!level.isLeaf()) {
        return
    }
    function("toArgumentStrings") {
        addModifiers(KModifier.OVERRIDE)
        if (parentClass == null) {
            addModifiers(KModifier.OPEN)
        }
        returns(listTypeNameOf<String>())
        addStatement("val arguments = toCompilerArguments().compilerToArgumentStrings()")
        addStatement("return arguments")
    }
}

private fun toCompilerConverterFunBuilder(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
): FunSpec.Builder = FunSpec.builder("toCompilerArguments").apply {
    val compilerArgumentsClass = level.getCompilerArgumentsClassName()
    addParameter(
        ParameterSpec.builder("arguments", compilerArgumentsClass).apply {
            if (level.isLeaf()) {
                defaultValue("%T()", compilerArgumentsClass)
            }
        }.build()
    )
    annotation<Suppress> {
        addMember("%S", "DEPRECATION")
    }
    if (parentClass != null) {
        addStatement("super.toCompilerArguments(arguments)")
    }
    addStatement("val unknownArgs = optionsMap.keys.filter { it !in knownArguments }")
    addCode(
        CodeBlock.builder()
            .beginControlFlow("if (unknownArgs.isNotEmpty())")
            .addStatement(
                "throw %T(\"Unknown arguments: \${unknownArgs.joinToString()}\")",
                IllegalStateException::class
            )
            .endControlFlow()
            .build()
    )
    returns(compilerArgumentsClass)
}

private fun TypeSpec.Builder.maybeAddApplyArgumentStringsFun(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
) {
    if (!level.isLeaf()) {
        return
    }
    function("applyArgumentStrings") {
        addModifiers(KModifier.OVERRIDE)
        if (parentClass == null) {
            addModifiers(KModifier.OPEN)
        }
        val compilerArgumentsClass = level.getCompilerArgumentsClassName()
        addParameter("arguments", listTypeNameOf<String>())
        addStatement(
            "val compilerArgs: %T = %M(arguments)",
            compilerArgumentsClass,
            MemberName("org.jetbrains.kotlin.cli.common.arguments", "parseCommandLineArguments")
        )
        addStatement(
            "%M(compilerArgs.errors)?.let { throw %M(it) }",
            MemberName("org.jetbrains.kotlin.cli.common.arguments", "validateArguments"),
            MemberName("org.jetbrains.kotlin.buildtools.api", "CompilerArgumentsParseException"),
        )
        addStatement("applyCompilerArguments(compilerArgs)")
    }
}

private fun applyCompilerArgumentsFunBuilder(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
): FunSpec.Builder = FunSpec.builder("applyCompilerArguments").apply {
    if (parentClass != null) {
        addStatement("super.applyCompilerArguments(arguments)")
    }
    val compilerArgumentsClass = level.getCompilerArgumentsClassName()
    addParameter("arguments", compilerArgumentsClass)
    annotation<Suppress> {
        addMember("%S", "DEPRECATION")
    }
}

private fun KotlinCompilerArgumentsLevel.getCompilerArgumentsClassName(): ClassName {
    val compilerArgumentInfo = levelToClassNameMap.getValue(name)
    val compilerArgumentsClass = ClassName(compilerArgumentInfo.classPackage, compilerArgumentInfo.className)
    return compilerArgumentsClass
}

private fun FunSpec.Builder.addSafeMethodAccessStatement(
    codeBlock: CodeBlock,
    failOnNoSuchMethod: Boolean = true,
    errorMessage: CodeBlock? = null,
): FunSpec.Builder {
    return if (failOnNoSuchMethod) {
        addStatement(
            "try { %L } catch (e: NoSuchMethodError) { throw IllegalStateException(%L).initCause(e) }",
            codeBlock,
            errorMessage ?: CodeBlock.of("%S", "Unknown parameter")
        )
    } else {
        addStatement(
            "try { %L } catch (_: NoSuchMethodError) {  }",
            codeBlock
        )
    }
}