/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.IntType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.cli.arguments.generator.calculateName
import org.jetbrains.kotlin.cli.arguments.generator.levelToClassNameMap
import org.jetbrains.kotlin.generators.kotlinpoet.annotation
import org.jetbrains.kotlin.generators.kotlinpoet.function
import org.jetbrains.kotlin.generators.kotlinpoet.listTypeNameOf
import org.jetbrains.kotlin.generators.kotlinpoet.property
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

internal class BtaImplGenerator(
    private val targetPackage: String,
    private val skipXX: Boolean,
    private val kotlinVersion: KotlinReleaseVersion,
) : BtaGenerator {

    private val outputs = mutableListOf<Pair<Path, String>>()

    override fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: TypeName?): GeneratorOutputs {
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
            addType(
                TypeSpec.classBuilder(implClassName).apply {
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

                    val toCompilerConverterFun = toCompilerConverterFunBuilder(level, parentClass)
                    val applyCompilerArgumentsFun = applyCompilerArgumentsFunBuilder(level, parentClass)

                    val argumentTypeNameString = generateArgumentType(apiClassName)
                    val argumentTypeName = ClassName(API_ARGUMENTS_PACKAGE, apiClassName, argumentTypeNameString)
                    val argumentImplTypeName = ClassName(targetPackage, implClassName, argumentTypeNameString)

                    generateGetPutFunctions(argumentTypeName, argumentImplTypeName)
                    addType(TypeSpec.companionObjectBuilder().apply {
                        generateOptions(
                            arguments = level.arguments,
                            implClassName = implClassName,
                            argumentTypeName = argumentImplTypeName,
                            applyCompilerArgumentsFun = applyCompilerArgumentsFun,
                            toCompilerConverterFun = toCompilerConverterFun,
                        )
                    }.build())

                    if (level.isLeaf()) {
                        toCompilerConverterFun.addStatement(
                            "arguments.internalArguments = %M<%T>(internalArguments.toList()).internalArguments",
                            MemberName("org.jetbrains.kotlin.cli.common.arguments", "parseCommandLineArguments"),
                            level.getCompilerArgumentsClassName()
                        )
                    }
                    toCompilerConverterFun.addStatement("return arguments")
                    addFunction(toCompilerConverterFun.build())

                    applyCompilerArgumentsFun.addStatement("internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })")
                    addFunction(applyCompilerArgumentsFun.build())

                    maybeAddApplyArgumentStringsFun(level, parentClass)
                    maybeAddToArgumentsStringFun(level, parentClass)
                }.build()
            )
        }.build()
        mainFile.writeTo(mainFileAppendable)
        outputs += Path(mainFile.relativePath) to mainFileAppendable.toString()
        return GeneratorOutputs(ClassName(targetPackage, implClassName), outputs)
    }

    private fun TypeSpec.Builder.generateOptions(
        arguments: Collection<KotlinCompilerArgument>,
        implClassName: String,
        argumentTypeName: ClassName,
        applyCompilerArgumentsFun: FunSpec.Builder,
        toCompilerConverterFun: FunSpec.Builder,
    ) {
        arguments.forEach { argument ->
            val name = argument.extractName()
            if (skipXX && name.startsWith("XX_")) return@forEach

            // argument is newer than currently generated version, skip it
            if (argument.releaseVersionsMetadata.introducedVersion > kotlinVersion) {
                return@forEach
            }

            val wasRemoved = argument.releaseVersionsMetadata.removedVersion?.let { removedVersion ->
                // argument was removed in or before current version - 3, skip it entirely
                if (removedVersion <= getOldestSupportedVersion(kotlinVersion)) {
                    return@forEach
                }
                true
            } ?: false

            // argument was introduced in one of recent versions, so it might not exist in older supported version
            val wasIntroducedRecently = (argument.releaseVersionsMetadata.introducedVersion > getOldestSupportedVersion(kotlinVersion))

            // generate impl mirror of arguments
            val type = argument.valueType::class
                .supertypes.single { it.classifier == KotlinArgumentValueType::class }
                .arguments.first().type!!
            val argumentTypeParameter = when (val classifier = type.classifier) {
                is KClass<*> if classifier.isSubclassOf(Enum::class) && classifier in enumNameAccessors -> {
                    ClassName("$API_ARGUMENTS_PACKAGE.enums", classifier.simpleName!!)
                }
                else -> {
                    type.asTypeName()
                }
            }.copy(nullable = argument.valueType.isNullable.current)
            property(name, argumentTypeName.parameterizedBy(argumentTypeParameter)) {
                initializer("%T(%S)", argumentTypeName, name)
            }

            // add argument to the converter functions
            val member = MemberName(ClassName(targetPackage, implClassName, "Companion"), name)
            CodeBlock.builder().apply {
                add("if (%M in this) { ", member)
                val valueToAssign = CodeBlock.builder().apply {
                    add("get(%M)", member)
                    add(
                        when {
                            type.classifier in enumNameAccessors -> maybeGetNullabilitySign(argument) + ".stringValue"
                            argument.valueType is IntType -> maybeGetNullabilitySign(argument) + ".toString()"
                            else -> ""
                        }
                    )
                }.build()
                if (wasRemoved) {
                    add(
                        "arguments.%M(%S, %L)",
                        MemberName(targetPackage, "setUsingReflection", isExtension = true),
                        argument.calculateName(),
                        valueToAssign
                    )
                } else {
                    add("arguments.%N = %L", argument.calculateName(), valueToAssign)
                }
                add("}")
            }.build().also { setStatement ->
                toCompilerConverterFun.addSafeSetStatement(wasIntroducedRecently, wasRemoved, name, argument, setStatement)
            }

            applyCompilerArgumentsFun.addSafeMethodAccessStatement(CodeBlock.builder().apply {
                add("this[%M] = ", member)
                if (wasRemoved) {
                    add("arguments.%M(%S)", MemberName(targetPackage, "getUsingReflection", isExtension = true), argument.calculateName())
                } else {
                    add("arguments.%N", argument.calculateName())
                }

                when {
                    type.classifier in enumNameAccessors -> {
                        add(maybeGetNullabilitySign(argument))
                        add(".let { %T.entries.first { entry -> entry.stringValue == it } }", argumentTypeParameter.copy(nullable = false))
                    }
                    argument.valueType is IntType -> {
                        add(maybeGetNullabilitySign(argument))
                        add(".let { it.toInt() }")
                    }
                    else -> ""
                }
            }.build(), failOnNoSuchMethod = false)
        }
    }

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
            addModifiers(KModifier.OPERATOR)
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

private fun FunSpec.Builder.addSafeSetStatement(
    wasIntroducedRecently: Boolean,
    wasRemoved: Boolean,
    name: String,
    argument: KotlinCompilerArgument,
    setStatement: CodeBlock,
) {
    if (wasIntroducedRecently || wasRemoved) {
        val errorMessage = CodeBlock.of(
            "%P",
            buildString {
                append("Compiler parameter not recognized: $name. Current compiler version is: \$KC_VERSION}, but")
                if (wasIntroducedRecently) {
                    append(" argument was introduced in ${argument.releaseVersionsMetadata.introducedVersion.releaseName}")
                }
                if (wasRemoved) {
                    if (wasIntroducedRecently) {
                        append(" and")
                    }
                    append(" was removed in ${argument.releaseVersionsMetadata.removedVersion?.releaseName}")
                }
            }
        )
        addSafeMethodAccessStatement(setStatement, failOnNoSuchMethod = true, errorMessage = errorMessage)
    } else {
        addStatement("%L", setStatement)
    }
}

private fun maybeGetNullabilitySign(argument: KotlinCompilerArgument): String = (if (argument.valueType.isNullable.current) "?" else "")

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