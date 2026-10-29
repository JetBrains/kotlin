/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.ExperimentalArgumentApi
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.*
import org.jetbrains.kotlin.cli.arguments.generator.levelToClassNameMap
import org.jetbrains.kotlin.generators.kotlinpoet.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

private const val ARGUMENT_PARSE_DIAGNOSTICS_CLASS = "ArgumentParseDiagnostics"

@OptIn(ExperimentalArgumentApi::class)
internal class BtaImplOptionsGenerator(
    override val targetPackage: String,
    private val skipXX: Boolean,
    /**
     * The Kotlin version that is used for generating arguments from SSoT.
     */
    private val kotlinVersion: KotlinReleaseVersion,
) : BtaOptionsGenerator {

    private val outputs = mutableListOf<Pair<Path, String>>()

    private var currentFileCallsApiEnumValues = false

    override fun generateArgumentsForLevel(
        level: KotlinCompilerArgumentsLevel,
        parentClass: ClassName?,
        additionalInterfaces: List<ClassName>,
    ): GeneratorOutputs {
        val apiClassName = level.name.capitalizeAsciiOnly()
        val implClassName = apiClassName + "Impl"
        val mainFileAppendable = createGeneratedFileAppendable()
        val arguments = level.transformImplArguments().filter { argument ->
            val name = argument.getOptionId()
            if (skipXX && name.startsWith("XX_")) return@filter false

            // argument is newer than currently generated version, skip it
            if (argument.introducedSinceVersion > kotlinVersion) {
                return@filter false
            }

            if (argument.removedSinceVersion != null) {
                // argument was removed in or before current version - 3, skip it entirely
                return@filter false
            }
            true
        }

        currentFileCallsApiEnumValues = false
        val mainFileBuilder = FileSpec.builder(targetPackage, implClassName).apply {
            addAnnotation(AnnotationSpec.builder(ClassName("kotlinx.serialization", "UseSerializers")).apply {
                addMember("%T::class", ClassName("org.jetbrains.kotlin.buildtools.internal.serializability", "PathAsStringSerializer"))
            }.build())
            // Kotlinpoet requires these aliased imports when there's a name clash in the current context or else it calls the wrong member
            addAliasedImport(MemberName("org.jetbrains.kotlin.compilerRunner", "toArgumentStrings"), "compilerToArgumentStrings")
            addAliasedImport(MemberName(ClassName("org.jetbrains.kotlin.config", "KotlinCompilerVersion"), "VERSION"), "KC_VERSION")

            //@file:OptIn(ExperimentalCompilerArgument::class)
            addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .addMember("%T::class", ANNOTATION_EXPERIMENTAL).build()
            )
            classType(implClassName) {
                //class modifiers
                addModifiers(KModifier.INTERNAL)
                if (!level.isLeaf()) {
                    addModifiers(KModifier.ABSTRACT)
                }
                annotation(ANNOTATION_SERIALIZABLE)

                // superclass
                if (parentClass != null) {
                    superclass(parentClass)
                }
                // class interfaces
                val syntheticInterfaces = syntheticArgumentInterfaces.filter { it.concreteClassName == implClassName }
                if (syntheticInterfaces.isEmpty()) {
                    addSuperinterface(ClassName(API_ARGUMENTS_PACKAGE, level.name.capitalizeAsciiOnly()))
                    addSuperinterface(ClassName(API_ARGUMENTS_PACKAGE, level.name.capitalizeAsciiOnly()).nestedClass("Builder"))
                } else {
                    syntheticInterfaces.forEach {
                        addSuperinterface(ClassName(API_ARGUMENTS_PACKAGE, it.name))
                        addSuperinterface(ClassName(API_ARGUMENTS_PACKAGE, it.name).nestedClass("Builder"))
                    }
                }

                // constructors
                primaryConstructor(FunSpec.constructorBuilder().build())
                addFunction(getSecondaryConstructorFun())

                // properties
                if (parentClass == null) {
                    property(
                        "internalArguments",
                        ClassName("kotlin.collections", "MutableSet").parameterizedBy(typeNameOf<String>()),
                        KModifier.PROTECTED
                    ) {
                        initializer("%M()", MemberName("kotlin.collections", "mutableSetOf"))
                    }
                }

                property(
                    "defaultArguments",
                    level.getCompilerArgumentsClassName(),
                    KModifier.PRIVATE,
                ) {
                    annotation(ANNOTATION_TRANSIENT)
                    initializer("createDefaultArguments()")
                }

                // methods
                function("createDefaultArguments") {
                    returns(level.getCompilerArgumentsClassName())
                    if (parentClass != null) addModifiers(KModifier.OVERRIDE)
                    if (level.isLeaf()) {
                        addStatement("return %T()", level.getCompilerArgumentsClassName())
                    } else {
                        addModifiers(KModifier.ABSTRACT)
                    }
                }

                val toCompilerConverterFun = toCompilerConverterFunBuilder(level, parentClass, skipXX, kotlinVersion)
                val toCompilerArgumentsAffectingOutcomeFun = toCompilerArgumentsAffectingOutcomeFunBuilder(level, parentClass)
                val applyCompilerArgumentsFun = applyCompilerArgumentsFunBuilder(level, parentClass)

                val argumentTypeNameString = apiClassName.removeSuffix("s")
                val adapterClassName = ClassName(targetPackage, "${argumentTypeNameString}ValueAdapter")

                generateOptionsMap()

                if (syntheticInterfaces.isEmpty()) {
                    val argumentTypeName = ClassName(API_ARGUMENTS_PACKAGE, apiClassName, argumentTypeNameString)
                    generateGetPutFunctions(argumentTypeName, level, adapterClassName, arguments)
                } else {
                    syntheticInterfaces.forEach { syntheticInterface ->
                        val argumentTypeName =
                            ClassName(API_ARGUMENTS_PACKAGE, syntheticInterface.name, syntheticInterface.name.removeSuffix("s"))
                        generateGetPutFunctions(
                            argumentTypeName,
                            syntheticLevels.getValue(syntheticInterface.name),
                            adapterClassName,
                            arguments
                        )
                    }
                }

                val mirroredEnums = generateOptions(
                    arguments = arguments,
                    applyCompilerArgumentsFun = applyCompilerArgumentsFun,
                    toCompilerConverterFun = toCompilerConverterFun,
                    toCompilerArgumentsAffectingOutcomeFun = toCompilerArgumentsAffectingOutcomeFun,
                    level = level,
                    adapterClassName
                )

                outputs += generateValueAdapterFile(
                    adapterClassName,
                    mirroredEnums,
                    arguments.filterIsInstance<BtaCompilerArgument.CustomCompilerArgument>()
                )

                if (level.isLeaf()) {
                    function("deepCopy") {
                        addModifiers(KModifier.OVERRIDE)
                        returns(ClassName(targetPackage, implClassName))
                        val constructorArgs =
                            "argumentValidationErrors = argumentValidationErrors.toSet(), restrictedArgViolations = restrictedArgViolations.toList(), argumentParseDiagnostics = argumentParseDiagnostics.copy()"
                        addStatement(
                            "return %T($constructorArgs).also { newArgs -> newArgs.applyCompilerArguments(toCompilerArguments()) }",
                            ClassName(targetPackage, implClassName)
                        )
                    }
                    function("build") {
                        addModifiers(KModifier.OVERRIDE)
                        returns(ClassName(targetPackage, implClassName))
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
                    toCompilerConverterFun.addStatement(
                        "%M(arguments)",
                        MemberName("org.jetbrains.kotlin.buildtools.internal.arguments", "populateExplicitArguments")
                    )
                } else {
                    function("build") {
                        addModifiers(KModifier.OVERRIDE, KModifier.ABSTRACT)
                        returns(ClassName(targetPackage, implClassName))
                    }
                }

                toCompilerConverterFun.addStatement("return arguments")
                addFunction(toCompilerConverterFun.build())

                applyCompilerArgumentsFun.addStatement("internalArguments.addAll(arguments.internalArguments.map { it.stringRepresentation })")
                addFunction(applyCompilerArgumentsFun.build())

                toCompilerArgumentsAffectingOutcomeFun.addStatement("return arguments")
                addFunction(toCompilerArgumentsAffectingOutcomeFun.build())

                maybeAddApplyArgumentStringsFun(level, kotlinVersion)
                maybeAddApplyCommandLineArgumentsFun(level)
                maybeAddToArgumentsStringFun(level, parentClass)
                generateRestrictedArgViolationCollection(level, parentClass)
                generateToCompilationInputsFun(level)
            }
        }
        if (currentFileCallsApiEnumValues) {
            mainFileBuilder.addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "Suppress"))
                    .addMember("%S", "EnumValuesSoftDeprecate").build()
            )
        }
        val mainFile = mainFileBuilder.build()
        mainFile.writeTo(mainFileAppendable)
        outputs += Path(mainFile.relativePath) to mainFileAppendable.toString()
        return GeneratorOutputs(ClassName(targetPackage, implClassName), outputs)
    }

    /**
     * Generates the object converting argument values between their API and [targetPackage] representations.
     */
    private fun generateValueAdapterFile(
        adapterClassName: ClassName,
        mirroredEnums: Set<KClass<*>>,
        customArguments: List<BtaCompilerArgument.CustomCompilerArgument>,
    ): Pair<Path, String> {
        fun conversionFun(toApi: Boolean): FunSpec {
            val name = if (toApi) "toApi" else "toImpl"
            return FunSpec.builder(name).apply {
                annotation<Suppress> {
                    addMember("%S", "UNCHECKED_CAST")
                }
                addTypeVariable(TypeVariableName("T"))
                returns(TypeVariableName("T"))
                addParameter("value", ANY.copy(nullable = true))
                beginControlFlow("return when (value)")
                addStatement(
                    "is %T -> value.map { %N<T>(it) }",
                    LIST.parameterizedBy(STAR),
                    name,
                )
                mirroredEnums.forEach { enumType ->
                    val apiType = enumType.toBtaEnumClassName()
                    val implEnumType = enumType.toBtaImplEnumClassName(targetPackage)
                    val sourceType = if (toApi) implEnumType else apiType
                    val targetType = if (toApi) apiType else implEnumType
                    val converter = if (toApi) "toApiEnum" else "toImplEnum"
                    addStatement("is %T -> value.%N<%T>()", sourceType, converter, targetType)
                }
                customArguments.forEach { it: BtaCompilerArgument.CustomCompilerArgument ->
                    val apiType = when (val type = it.valueType.type) {
                        is ParameterizedTypeName if type.rawType == ClassName("kotlin.collections", "List") -> {
                            type.typeArguments.single() as ClassName
                        }
                        else -> type
                    }.copy(nullable = false)
                    val implType = getCustomParametersImplType(apiType).copy(nullable = false)
                    val sourceType = if (toApi) implType else apiType
                    val converter = if (toApi) "toApi" else "toImpl"
                    addStatement("is %T -> value.%N()", sourceType, converter)
                }
                addStatement("else -> value")
                endControlFlow()
                addCode("as T")
            }.build()
        }

        val appendable = createGeneratedFileAppendable()
        val fileSpec = FileSpec.builder(adapterClassName).apply {
            addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .addMember("%T::class", ANNOTATION_EXPERIMENTAL).build()
            )
            addType(TypeSpec.objectBuilder(adapterClassName).apply {
                addModifiers(KModifier.INTERNAL)
                addKdoc(
                    "Converts argument values between the representation used by the API and the one used by " +
                            "`$targetPackage`.\n\n" +
                            "A value needs converting whenever the two sides declare its type separately, as they do " +
                            "for the mirrored argument enums.\n"
                )
                addFunction(conversionFun(toApi = true))
                addFunction(conversionFun(toApi = false))
            }.build())
        }.build()
        fileSpec.writeTo(appendable)
        return Path(fileSpec.relativePath) to appendable.toString()
    }

    private fun getSecondaryConstructorFun(): FunSpec = FunSpec.constructorBuilder().apply {
        addParameter(
            ParameterSpec.builder("argumentValidationErrors", setTypeNameOf<String>())
                .defaultValue("%M()", MemberName("kotlin.collections", "emptySet"))
                .build()
        )

        addParameter(
            ParameterSpec.builder(
                "restrictedArgViolations",
                ClassName("kotlin.collections", "List")
                    .parameterizedBy(
                        ClassName(targetPackage, "RestrictedArgViolation")
                    )
            )
                .defaultValue("%M()", MemberName("kotlin.collections", "emptyList"))
                .build()
        )

        addParameter(
            ParameterSpec.builder("argumentParseDiagnostics", ClassName(targetPackage, ARGUMENT_PARSE_DIAGNOSTICS_CLASS))
                .defaultValue("%T()", ClassName(targetPackage, ARGUMENT_PARSE_DIAGNOSTICS_CLASS))
                .build()
        )
        callThisConstructor()
        addStatement("_argumentValidationErrors += argumentValidationErrors")
        addStatement("_restrictedArgViolations += restrictedArgViolations")
        addStatement("this.argumentParseDiagnostics += argumentParseDiagnostics")
    }.build()

    private fun TypeSpec.Builder.generateOptions(
        arguments: Collection<BtaCompilerArgument<*>>,
        applyCompilerArgumentsFun: FunSpec.Builder,
        toCompilerConverterFun: FunSpec.Builder,
        toCompilerArgumentsAffectingOutcomeFun: FunSpec.Builder,
        level: KotlinCompilerArgumentsLevel,
        adapterClassName: ClassName,
    ): Set<KClass<*>> {
        val knownOptionIds: CodeBlock.Builder = CodeBlock.builder()
        val enumsToGenerate = mutableMapOf<KClass<*>, TypeSpec.Builder>()

        /**
         * Marks enum to be generated and returns its name
         */
        fun generatedEnumType(type: KClass<*>): ClassName {
            require(WithStringRepresentation::class in type.allSuperclasses) {
                "Compiler enum ${type.qualifiedName} must implement ${WithStringRepresentation::class.qualifiedName} to be used with BTA."
            }
            val enumConstants = type.java.enumConstants.filterIsInstance<Enum<*>>()
            @Suppress("UNCHECKED_CAST")
            enumConstants as List<WithStringRepresentation>
            enumsToGenerate[type] =
                generateEnumTypeBuilder(enumConstants, level, type.toBtaImplEnumClassName(targetPackage))
            return type.toBtaImplEnumClassName(targetPackage)
        }

        arguments.forEach { argument ->
            val name = argument.getOptionId()

            // generate impl mirror of arguments
            val argumentTypeParameter = when (argument) {
                is BtaCompilerArgument.SSoTCompilerArgument -> {
                    val type = argument.valueType.kType
                    val classifier = type.classifier as? KClass<*> ?: error("Type is not a KClass: $type")
                    when {
                        classifier.java.isEnum -> {
                            generatedEnumType(classifier)
                        }
                        classifier == List::class && (type.arguments.first().type?.classifier as? KClass<*>)?.java?.isEnum == true -> {
                            listTypeNameOf(generatedEnumType(type.arguments.first().type?.classifier as KClass<*>))
                        }
                        else -> {
                            type.asTypeName()
                        }
                    }
                }
                is BtaCompilerArgument.CustomCompilerArgument -> argument.valueType.type.let { type ->
                    getCustomParametersImplType(type)
                }
            }.copy(nullable = argument.valueType.isNullable)

            val argumentProperty = PropertySpec.builder(argument.effectiveCompilerName, argumentTypeParameter, KModifier.INTERNAL).apply {
                mutable(true)
                annotation(ANNOTATION_SERIAL_NAME) {
                    addMember("%S", name)
                }
                if (argument.deprecatedSinceVersion != null) {
                    annotation<Suppress> {
                        addMember("%S", "DEPRECATION")
                    }
                }
            }

            knownOptionIds.add("%S,", name)

            when (argument) {
                is BtaCompilerArgument.SSoTCompilerArgument -> {
                    generateAutomaticArgumentsPropagators(
                        argumentTypeParameter,
                        argument,
                        argument.effectiveCompilerName,
                        toCompilerConverterFun,
                        toCompilerArgumentsAffectingOutcomeFun,
                        applyCompilerArgumentsFun,
                        argumentTypeParameter,
                        argumentProperty
                    )
                }

                is BtaCompilerArgument.CustomCompilerArgument -> {
                    generateCustomRepresentation(
                        argument,
                        toCompilerConverterFun,
                        toCompilerArgumentsAffectingOutcomeFun,
                        applyCompilerArgumentsFun,
                        argumentProperty,
                        adapterClassName
                    )
                }
            }

            addProperty(argumentProperty.build())
        }

        addType(TypeSpec.companionObjectBuilder().apply {
            property(
                "knownOptionIds",
                ClassName("kotlin.collections", "List").parameterizedBy(
                    String::class.asClassName(),
                ),
                KModifier.PRIVATE
            ) {
                initializer(CodeBlock.builder().apply {
                    add("%M(%L)", MemberName("kotlin.collections", "listOf"), knownOptionIds.build())
                }.build())
            }
        }.build())

        enumsToGenerate.forEach { [type, typeSpecBuilder] ->
            outputs += writeEnumFile(typeSpecBuilder.build(), type, targetPackage)
        }
        return enumsToGenerate.keys
    }

    private fun generateCustomRepresentation(
        argument: BtaCompilerArgument.CustomCompilerArgument,
        toCompilerConverterFun: FunSpec.Builder,
        toCompilerArgumentsAffectingOutcomeFun: FunSpec.Builder,
        applyCompilerArgumentsFun: FunSpec.Builder,
        argumentProperty: PropertySpec.Builder,
        adapterClassName: ClassName,
    ) {
        val applier = MemberName(targetPackage, argument.applierSimpleName)

        CodeBlock.builder().apply {
            add("arguments.%M(%N)", applier, argumentProperty.build())
        }.build().also { setStatement ->
            // There's no need in future compatibility check for non-compat layer.
            // The main impl is tied to a compiler version and could not know about the future changes.
            toCompilerConverterFun.addStatement(
                "%L", setStatement
            )
            if (argument.affectsCompilationOutcome) {
                // There's no need in future compatibility check for non-compat layer.
                // The main impl is tied to a compiler version and could not know about the future changes.
                toCompilerArgumentsAffectingOutcomeFun.addStatement(
                    "%L", setStatement
                )
            }
        }

        applyCompilerArgumentsFun.addSafeMethodAccessStatement(
            CodeBlock.builder().apply {
                add("%N = %M(%N, arguments)", argumentProperty.build(), applier, argumentProperty.build())
            }.build(),
            catches =
                buildList {
                    add(catchCompilerArgumentsParseException())
                },
        )
        argumentProperty.initializer(
            CodeBlock.of(
                "@Suppress(\"UNCHECKED_CAST\") %M(%T.toImpl(%L) as %T, defaultArguments)",
                applier,
                adapterClassName,
                argument.defaultValue,
                argumentProperty.build().type
            )
        )
    }

    /**
     * Generates code that configures for example `org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments` from `org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments` and vice versa
     */
    private fun generateAutomaticArgumentsPropagators(
        type: TypeName,
        argument: BtaCompilerArgument.SSoTCompilerArgument,
        effectiveCompilerName: String,
        toCompilerConverterFun: FunSpec.Builder,
        toCompilerArgumentsAffectingOutcomeFun: FunSpec.Builder,
        applyCompilerArgumentsFun: FunSpec.Builder,
        argumentTypeParameter: TypeName,
        argumentProperty: PropertySpec.Builder,
    ) {
        // BTA → Compiler conversion
        CodeBlock.builder().apply {
            val valueToAssign = buildBtaToCompilerValueTransform(argumentProperty, type, argument)
            val assignment = buildCompilerAssignment(effectiveCompilerName, valueToAssign)
            add("%L", assignment)
        }.build().also { setStatement ->
            // There's no need in future compatibility check for non-compat layer.
            // The main impl is tied to a compiler version and could not know about the future changes.
            toCompilerConverterFun.addStatement(
                "%L", setStatement
            )
            if (argument.affectsCompilationOutcome) {
                // There's no need in future compatibility check for non-compat layer.
                // The main impl is tied to a compiler version and could not know about the future changes.
                toCompilerArgumentsAffectingOutcomeFun.addStatement(
                    "%L", setStatement
                )
            }
        }

        // Compiler → BTA conversion
        val compilerToBtaStatement = buildCompilerToBtaValueTransform(
            type, argument, effectiveCompilerName, argumentTypeParameter, argumentProperty
        )
        applyCompilerArgumentsFun.addSafeMethodAccessStatement(
            compilerToBtaStatement,
            catches = buildList {
                if (type.isGeneratedEnum || type.isGeneratedEnumList()) {
                    add(catchCompilerArgumentsParseException())
                }
            },
        )
        argumentProperty.initializer(
            buildCompilerToBtaValueTransform(
                type, argument, effectiveCompilerName, argumentTypeParameter, argumentProperty, true
            )
        )
    }

    /**
     * Builds the value transformation from BTA to compiler (e.g., enum.stringValue, int.toString(), path.absolutePathStringOrThrow())
     */
    private fun buildBtaToCompilerValueTransform(
        argumentProperty: PropertySpec.Builder,
        type: TypeName,
        argument: BtaCompilerArgument<BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType>,
    ): CodeBlock = CodeBlock.builder().apply {
        add("%N", argumentProperty.build())
        when {
            type.isGeneratedEnum -> {
                add(maybeGetNullabilitySign(argument) + ".stringValue")
            }
            type.isGeneratedEnumList() -> {
                add(maybeGetNullabilitySign(argument) + ".map { it.stringValue }.toTypedArray()")
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
            argument.valueType.origin is StringArrayType -> {
                maybeAddDelimiterValidation(argument)
                add(" ?: emptyArray()")
            }
            argument.valueType.origin is StringListType -> {
                maybeAddDelimiterValidation(argument)
                add(
                    maybeGetNullabilitySign(argument) + ".%M()",
                    MemberName(KOTLIN_COLLECTIONS, "toTypedArray")
                )
            }
            argument.valueType.origin is SearchPathType -> {
                add(
                    maybeGetNullabilitySign(argument) + ".%M { it.%M() }",
                    MemberName(KOTLIN_COLLECTIONS, "map"),
                    MemberName(targetPackage, "absolutePathStringOrThrow", true),
                )
                maybeAddDelimiterValidation(argument)
                add(
                    maybeGetNullabilitySign(argument) + ".%M(%T.pathSeparator)",
                    MemberName(KOTLIN_COLLECTIONS, "joinToString"),
                    ClassName(JAVA_IO, "File")
                )
            }
            argument.valueType.origin is PathListType -> {
                add(
                    maybeGetNullabilitySign(argument) + ".%M { it.%M() }",
                    MemberName(KOTLIN_COLLECTIONS, "map"),
                    MemberName(targetPackage, "absolutePathStringOrThrow", true),
                )
                maybeAddDelimiterValidation(argument)
                add(
                    maybeGetNullabilitySign(argument) + ".%M()",
                    MemberName(KOTLIN_COLLECTIONS, "toTypedArray")
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
        valueToAssign: CodeBlock,
    ): CodeBlock = CodeBlock.builder().apply {
        add("arguments.%N = %L", effectiveCompilerName, valueToAssign)
    }.build()

    /**
     * Builds the value transformation from compiler to BTA (e.g., string to enum, string.toInt(), path parsing)
     */
    private fun buildCompilerToBtaValueTransform(
        type: TypeName,
        argument: BtaCompilerArgument<BtaCompilerArgumentValueType.SSoTCompilerArgumentValueType>,
        effectiveCompilerName: String,
        argumentTypeParameter: TypeName,
        argumentProperty: PropertySpec.Builder,
        forInitializer: Boolean = false,
    ): CodeBlock = CodeBlock.builder().apply {
        if (!forInitializer) {
            add("%N = ", argumentProperty.build())
        }
        val argumentsName = if (forInitializer) "defaultArguments" else "arguments"
        add("$argumentsName.%N", effectiveCompilerName)

        when {
            // BTA-API and BTA-IMPL could be used with different Kotlin-stdlib versions at the runtime, but from the different classloaders,
            // which lead to JDK linkage error for `EnumEntries` symbol. We avoiding it by explicitly using values() method instead.
            type.isGeneratedEnum -> {
                currentFileCallsApiEnumValues = true
                add(maybeGetNullabilitySign(argument))
                add(
                    $$".let { %T.values().firstOrNull { entry -> entry.stringValue.equals(it, true) }?.also { entry -> %M(_restrictedArgViolations, $$argumentsName::%N, entry.stringValue, it) } ?: throw %M(\"Unknown -$${argument.name} value: $it\") }",
                    argumentTypeParameter.copy(nullable = false),
                    MemberName(targetPackage, "checkCaseMatches"),
                    effectiveCompilerName,
                    MemberName("org.jetbrains.kotlin.buildtools.api", "CompilerArgumentsParseException"),
                )
            }
            type.isGeneratedEnumList() -> {
                currentFileCallsApiEnumValues = true
                val enumType = type.typeArguments[0]
                add(
                    $$".map { %T.values().firstOrNull { entry -> entry.stringValue == it } ?: throw %M(\"Unknown -$${argument.name} value: $it\") }",
                    enumType.copy(nullable = false),
                    MemberName("org.jetbrains.kotlin.buildtools.api", "CompilerArgumentsParseException")
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
            argument.valueType.origin is StringListType -> {
                add(
                    maybeGetNullabilitySign(argument) + ".%M()",
                    MemberName(targetPackage, "toListOrEmpty", true)
                )
            }
            argument.valueType.origin is SearchPathType -> {
                add(
                    maybeGetNullabilitySign(argument) + ".%M(%T.pathSeparator)" + maybeGetNullabilitySign(argument) + ".%M { %M(it) }",
                    MemberName(KOTLIN_TEXT, "split", true),
                    ClassName(JAVA_IO, "File"),
                    MemberName(KOTLIN_COLLECTIONS, "map"),
                    MemberName(KOTLIN_IO_PATH, "Path")
                )
            }
            argument.valueType.origin is PathListType -> {
                add(
                    maybeGetNullabilitySign(argument) + ".%M { %M(it) }",
                    MemberName(targetPackage, "mapOrEmpty", true),
                    MemberName(KOTLIN_IO_PATH, "Path")
                )
            }
            else -> add("")
        }
    }.build()

    private fun TypeSpec.Builder.generateToCompilationInputsFun(
        level: KotlinCompilerArgumentsLevel,
    ) {
        if (!level.isLeaf()) return
        function("toCompilationInputs") {
            addKdoc(
                """
                Returns a sorted list of compiler argument strings representing only the arguments
                that affect the compilation outcome (i.e. those with [affectsCompilationOutcome][org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument.affectsCompilationOutcome] set to true).
                Arguments with default values are omitted from the output, because [toCompilerArgumentsAffectingOutcome]
                only sets arguments that have been explicitly assigned, and [compilerToArgumentStrings][org.jetbrains.kotlin.compilerRunner.toArgumentStrings]
                skips properties whose value matches the default.
                """.trimIndent()
            )
            returns(listTypeNameOf<String>())
            addStatement("return toCompilerArgumentsAffectingOutcome().compilerToArgumentStrings(allowArgFileInValues = false).sorted()")
        }
    }

    private fun TypeSpec.Builder.generateOptionsMap(): PropertySpec = property(
        "optionsMap",
        ClassName("kotlin.collections", "MutableMap").parameterizedBy(typeNameOf<String>(), ANY.copy(nullable = true))
    ) {
        addModifiers(KModifier.PRIVATE)
        annotation(ANNOTATION_TRANSIENT)
        initializer("%M()", MemberName("kotlin.collections", "mutableMapOf"))
    }

    fun TypeSpec.Builder.generateGetPutFunctions(
        parameter: ClassName,
        level: KotlinCompilerArgumentsLevel,
        adapterClassName: ClassName,
        arguments: List<BtaCompilerArgument<*>>,
    ) {
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
            beginControlFlow("return %T.toApi(when (key.id)", adapterClassName)
            arguments.forEach { argument ->
                addStatement("%S -> %L", argument.getOptionId(), argument.effectiveCompilerName)
            }
            addStatement(
                $$"""
                else -> {
                    check(key.id in optionsMap) { "Argument ${key.id} is not set and has no default value" }
                    optionsMap[key.id]
                }
            """.trimIndent()
            )
            endControlFlow()
            addCode(")")
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

            val currentKotlinVersion = CodeBlock.of(
                "(%L, %L, %L)",
                kotlinVersion.major,
                kotlinVersion.minor,
                kotlinVersion.patch
            )
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
            beginControlFlow("when (key.id)")
            arguments.forEach { argument ->
                addStatement("%S -> %L = %T.toImpl(value)", argument.getOptionId(), argument.effectiveCompilerName, adapterClassName)
            }
            addStatement(
                """
                else -> {
                    optionsMap[key.id] = %T.toImpl(value)
                }
            """.trimIndent(),
                adapterClassName
            )
            endControlFlow()
        }

        if (levelsSince[level.name] == KDOC_SINCE_2_3_0) {
            withDeprecationCycle(
                kotlinVersion,
                warnFrom = KotlinReleaseVersion.v2_4_0,
                errorFrom = KotlinReleaseVersion.v2_5_0,
                removeFrom = KotlinReleaseVersion.v2_6_0,
                deprecationMessage = "This method is no longer useful when compiling with Kotlin compiler 2.3.20 and above, as the arguments instance now contains default values for all arguments."
            ) { annotation ->
                function("contains") {
                    annotation?.let { addAnnotation(it) }
                    addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
                    returns(BOOLEAN)
                    addParameter("key", parameter.parameterizedBy(STAR))
                    addStatement("return key.id in knownOptionIds")
                }
            }
        }
    }

    private fun TypeSpec.Builder.generateRestrictedArgViolationCollection(
        level: KotlinCompilerArgumentsLevel,
        parentClass: ClassName?,
    ) {
        val restrictedArgViolationClass = ClassName(targetPackage, "RestrictedArgViolation")
        val rootCompilerArgsClass = kotlinCompilerArguments.topLevel.getCompilerArgumentsClassName()
        val ownViolationInfos = collectRestrictedArgInfo(level)

        if (parentClass == null) {
            property(
                "_restrictedArgViolations",
                ClassName("kotlin.collections", "MutableList").parameterizedBy(restrictedArgViolationClass),
                KModifier.PROTECTED,
            ) {
                annotation(ANNOTATION_TRANSIENT)
                initializer(
                    "%M()",
                    MemberName("kotlin.collections", "mutableListOf"),
                )
            }
            addProperty(
                PropertySpec.builder(
                    "restrictedArgViolations",
                    ClassName("kotlin.collections", "List").parameterizedBy(restrictedArgViolationClass),
                )
                    .addModifiers(KModifier.INTERNAL)
                    .getter(FunSpec.getterBuilder().addStatement("return _restrictedArgViolations").build())
                    .build()
            )
            property(
                "_argumentValidationErrors",
                ClassName("kotlin.collections", "MutableSet").parameterizedBy(typeNameOf<String>()),
                KModifier.PROTECTED,
            ) {
                initializer(
                    "%M()",
                    MemberName("kotlin.collections", "mutableSetOf"),
                )
            }
            addProperty(
                PropertySpec.builder(
                    "argumentValidationErrors",
                    ClassName("kotlin.collections", "Set").parameterizedBy(typeNameOf<String>()),
                )
                    .addModifiers(KModifier.INTERNAL)
                    .getter(FunSpec.getterBuilder().addStatement("return _argumentValidationErrors").build())
                    .build()
            )
            addProperty(
                PropertySpec.builder("argumentParseDiagnostics", ClassName(targetPackage, ARGUMENT_PARSE_DIAGNOSTICS_CLASS))
                    .addModifiers(KModifier.INTERNAL)
                    .initializer("%T()", ClassName(targetPackage, ARGUMENT_PARSE_DIAGNOSTICS_CLASS))
                    .addAnnotation(ANNOTATION_TRANSIENT)
                    .build()
            )
            function("collectRestrictedArgViolations") {
                addModifiers(KModifier.INTERNAL, KModifier.OPEN)
                addParameter("compilerArgs", rootCompilerArgsClass)
                addParameter("defaultArgs", rootCompilerArgsClass)
                addStatement("_restrictedArgViolations.clear()")
                if (ownViolationInfos.isNotEmpty()) {
                    for (info in ownViolationInfos) {
                        addViolationCheckStatement(info, "compilerArgs", "defaultArgs", restrictedArgViolationClass)
                    }
                }
            }
        } else {
            val levelCompilerArgsClass = level.getCompilerArgumentsClassName()
            val hasActiveViolations = ownViolationInfos.any { info ->
                (info.errorSince != null && kotlinVersion >= info.errorSince) || kotlinVersion >= info.warningSince
            }
            if (hasActiveViolations) {
                function("collectRestrictedArgViolations") {
                    addModifiers(KModifier.INTERNAL, KModifier.OVERRIDE)
                    addAnnotation(
                        AnnotationSpec.builder(ClassName("kotlin", "Suppress")).addMember("%S", "DEPRECATION").build()
                    )
                    addParameter("compilerArgs", rootCompilerArgsClass)
                    addParameter("defaultArgs", rootCompilerArgsClass)
                    addStatement("super.collectRestrictedArgViolations(compilerArgs, defaultArgs)")
                    addStatement("val args = compilerArgs as %T", levelCompilerArgsClass)
                    addStatement("val castedDefaults = defaultArgs as %T", levelCompilerArgsClass)
                    for (info in ownViolationInfos) {
                        addViolationCheckStatement(info, "args", "castedDefaults", restrictedArgViolationClass)
                    }
                }
            }
        }
    }

    private fun FunSpec.Builder.addViolationCheckStatement(
        info: RestrictedArgInfo,
        argsVarName: String,
        defaultsVarName: String,
        restrictedArgViolationClass: ClassName,
    ) {
        val namesStr = listOfNotNull(info.primaryCli, info.shortName, info.deprecatedName)
            .joinToString("/") { "'$it'" }
        val baseMessage = buildString {
            append("Argument $namesStr is not supported in the Build Tools API.")
            if (info.reason != null) append(" ${info.reason}")
        }
        val warningMessage = buildString {
            append(baseMessage)
            if (info.errorSince != null) {
                append(" This warning will become an error starting from Kotlin ${info.errorSince.releaseName}.")
            }
        }
        val [violationType, message] = when {
            info.errorSince != null && kotlinVersion >= info.errorSince -> "Error" to baseMessage
            kotlinVersion >= info.warningSince -> "Warning" to warningMessage
            else -> return
        }
        addCode(
            CodeBlock.of(
                "if (%L.%N != %L.%N) _restrictedArgViolations.add(%T.%L(%S))\n",
                argsVarName, info.fieldName, defaultsVarName, info.fieldName,
                restrictedArgViolationClass, violationType, message,
            )
        )
    }

    private fun CodeBlock.Builder.maybeAddDelimiterValidation(argument: BtaCompilerArgument<*>) {
        if (argument.delimiter == null) {
            return
        }

        add(
            maybeGetNullabilitySign(argument) + ".also { list -> list.%M(",
            MemberName(targetPackage, "checkNoneContains", isExtension = true)
        )
        add(argument.delimiter)
        add(") }")
    }

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
            addStatement("val arguments = toCompilerArguments().compilerToArgumentStrings(allowArgFileInValues = false)")
            addStatement("return arguments")
        }
    }

    private val TypeName.isGeneratedEnum: Boolean get() = (this as? ClassName)?.packageName?.startsWith("$targetPackage.enums") ?: false
    private fun TypeName.isGeneratedEnumList(): Boolean {
        @OptIn(ExperimentalContracts::class)
        contract {
            returns(true) implies (this@isGeneratedEnumList is ParameterizedTypeName)
        }
        return this is ParameterizedTypeName && this.rawType == List::class.asTypeName() && this.typeArguments[0].isGeneratedEnum
    }
}

private fun getCustomParametersImplType(type: TypeName): TypeName = when (type) {
    is ParameterizedTypeName if type.rawType == ClassName("kotlin.collections", "List") -> {
        val innerType = type.typeArguments.single() as ClassName
        listTypeNameOf(
            ClassName(
                innerType.packageName.replace(".api.arguments", ".internal.arguments"),
                innerType.simpleName + "Impl"
            ).copy(nullable = innerType.isNullable)
        )
    }
    is ClassName -> ClassName(type.packageName.replace(".api.arguments", ".internal.arguments"), type.simpleName + "Impl")
    else -> type
}

private fun maybeGetNullabilitySign(argument: BtaCompilerArgument<*>): String = (if (argument.valueType.isNullable) "?" else "")

private fun toCompilerArgumentsAffectingOutcomeFunBuilder(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
): FunSpec.Builder = FunSpec.builder("toCompilerArgumentsAffectingOutcome").apply {
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
        addStatement("super.toCompilerArgumentsAffectingOutcome(arguments)")
    }
    returns(compilerArgumentsClass)
}

private fun toCompilerConverterFunBuilder(
    level: KotlinCompilerArgumentsLevel,
    parentClass: TypeName?,
    skipXX: Boolean,
    kotlinVersion: KotlinReleaseVersion,
): FunSpec.Builder = FunSpec.builder("toCompilerArguments").apply {
    val compilerArgumentsClass = level.getCompilerArgumentsClassName()
    if (!level.isLeaf()) {
        addParameter("arguments", compilerArgumentsClass)
    } else {
        addStatement("val arguments = %T()", compilerArgumentsClass)
    }
    annotation<Suppress> {
        addMember("%S", "DEPRECATION")
    }
    if (parentClass != null) {
        addStatement("super.toCompilerArguments(arguments)")
    }
    val removedArguments = level.transformApiArguments().filter { argument ->
        val name = argument.getOptionId()
        argument.removedSinceVersion != null && (!skipXX || !name.startsWith("XX_"))
    }.distinctBy { it.getOptionId() }

    if (removedArguments.isNotEmpty()) {
        addCode(
            CodeBlock.builder().apply {
                beginControlFlow("val unknownArgs = optionsMap.keys.filter")
                beginControlFlow("when (it)")
                for (argument in removedArguments) {
                    val name = argument.getOptionId()
                    val wasIntroducedRecently = argument.introducedSinceVersion > getOldestSupportedVersion(kotlinVersion)
                    val errorMessage = CodeBlock.of(
                        "%P",
                        buildString {
                            append($$"Compiler parameter not recognized: $$name. Current compiler version is: $KC_VERSION, but")
                            if (wasIntroducedRecently) {
                                append(" the argument was introduced in ${argument.introducedSinceVersion.releaseName}")
                            }
                            append(if (wasIntroducedRecently) " and" else " the argument was")
                            append(" removed in ${argument.removedSinceVersion?.releaseName}")
                        }
                    )
                    addStatement("%S -> throw %T(%L)", name, IllegalStateException::class, errorMessage)
                }
                addStatement("else -> true")
                endControlFlow()
                endControlFlow()
            }.build()
        )
    } else addStatement("val unknownArgs = optionsMap.keys")
    addCode(
        CodeBlock.builder()
            .beginControlFlow("if (unknownArgs.isNotEmpty())")
            .addStatement(
                $$"throw %T(\"Unknown arguments: ${unknownArgs.joinToString()}\")",
                IllegalStateException::class
            )
            .endControlFlow()
            .build()
    )
    returns(compilerArgumentsClass)
}

private fun TypeSpec.Builder.maybeAddApplyArgumentStringsFun(
    level: KotlinCompilerArgumentsLevel,
    kotlinVersion: KotlinReleaseVersion,
) {
    if (!level.isLeaf()) {
        return
    }
    val compilerArgumentsClass = level.getCompilerArgumentsClassName()
    withDeprecationCycle(
        kotlinVersion,
        warnFrom = KotlinReleaseVersion.v2_5_0,
        errorFrom = KotlinReleaseVersion.v2_6_0,
        removeFrom = KotlinReleaseVersion.v2_7_0,
        deprecationMessage = "This method is deprecated. Use applyCommandLineArguments instead."
    ) { annotation ->
        function("applyArgumentStrings") {
            addModifiers(KModifier.OVERRIDE)
            annotation?.let { addAnnotation(it) }
            addParameter("arguments", listTypeNameOf<String>())
            addStatement(
                "val compilerArgs: %T = %M(arguments)",
                compilerArgumentsClass,
                MemberName("org.jetbrains.kotlin.cli.common.arguments", "parseCommandLineArguments")
            )
            addStatement("collectRestrictedArgViolations(compilerArgs, %T())", compilerArgumentsClass)
            addStatement(
                "%M(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }",
                MemberName("org.jetbrains.kotlin.cli.common.arguments", "validateArgumentsAllErrors"),
            )
            // has to run before the values are applied, so that values previously set through the typed argument API
            // are still observable
            addStatement("argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }")
            addStatement("applyCompilerArguments(compilerArgs)")
        }
    }
}

private fun TypeSpec.Builder.maybeAddApplyCommandLineArgumentsFun(
    level: KotlinCompilerArgumentsLevel,
) {
    if (!level.isLeaf()) {
        return
    }
    val compilerArgumentsClass = level.getCompilerArgumentsClassName()

    function("applyCommandLineArguments") {
        addAnnotation(ANNOTATION_DELICATE_BUILDTOOLS_API)
        addModifiers(KModifier.OVERRIDE)
        addParameter("arguments", listTypeNameOf<String>())
        addStatement("val compilerArgs = toCompilerArguments()")
        addStatement(
            "%M(arguments, compilerArgs, false)",
            MemberName("org.jetbrains.kotlin.cli.common.arguments", "parseCommandLineArguments")
        )
        addStatement(
            "%M(this, compilerArgs)",
            MemberName("org.jetbrains.kotlin.buildtools.internal.arguments", "handleCustomPluginArguments")
        )
        addStatement("collectRestrictedArgViolations(compilerArgs, %T())", compilerArgumentsClass)
        addStatement(
            "%M(compilerArgs.errors).forEach { _argumentValidationErrors.add(it) }",
            MemberName("org.jetbrains.kotlin.cli.common.arguments", "validateArgumentsAllErrors"),
        )
        // has to run before the values are applied, so that values previously set through the typed argument API
        // are still observable
        addStatement("argumentParseDiagnostics.record(compilerArgs, arguments) { toCompilerArguments() }")
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
    addModifiers(KModifier.PROTECTED)
}

private fun KotlinCompilerArgumentsLevel.getCompilerArgumentsClassName(): ClassName {
    val compilerArgumentInfo = levelToClassNameMap.getValue(name)
    val compilerArgumentsClass = ClassName(compilerArgumentInfo.classPackage, compilerArgumentInfo.className)
    return compilerArgumentsClass
}

private fun FunSpec.Builder.addSafeMethodAccessStatement(
    codeBlock: CodeBlock,
    catches: List<CodeBlock>,
): FunSpec.Builder {
    if (catches.isEmpty()) {
        return addStatement("%L", codeBlock)
    }
    val format = buildString {
        append("try { %L }")
        repeat(catches.size) { append(" %L") }
    }
    return addStatement(format, codeBlock, *catches.toTypedArray())
}

private fun catchCompilerArgumentsParseException(): CodeBlock = CodeBlock.of(
    "catch (ex: %M) { _argumentValidationErrors.add(ex.message ?: %S) }",
    MemberName("org.jetbrains.kotlin.buildtools.api", "CompilerArgumentsParseException"),
    "Error parsing compiler arguments",
)

