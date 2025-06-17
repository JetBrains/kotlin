/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.generators.kotlinpoet.annotation
import org.jetbrains.kotlin.generators.kotlinpoet.function
import org.jetbrains.kotlin.generators.kotlinpoet.property
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

class BtaApiGenerator() : BtaGenerator {
    private val outputs = mutableListOf<Pair<Path, String>>()

    override fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: TypeName?, skipXX: Boolean): GeneratorOutputs {
        val className = level.name.capitalizeAsciiOnly()
        val mainFileAppendable = createGeneratedFileAppendable()
        val mainFile = FileSpec.builder(API_PACKAGE, className).apply {
            addType(
                TypeSpec.interfaceBuilder(className).apply {
                    addKdoc(KDOC_SINCE_2_3_0)
                    if (level.name in experimentalLevelNames) {
                        addAnnotation(ANNOTATION_EXPERIMENTAL)
                    }
                    parentClass?.let { addSuperinterface(it) }
                    val argument = generateArgumentType(className)
                    val argumentTypeName = ClassName(API_PACKAGE, className, argument)
                    generateGetPutFunctions(argumentTypeName)
                    addType(TypeSpec.companionObjectBuilder().apply {
                        generateOptions(level.filterOutDroppedArguments(), argumentTypeName, skipXX)
                    }.build())
                }.build()
            )
        }.build()
        mainFile.writeTo(mainFileAppendable)
        outputs += Path(mainFile.relativePath) to mainFileAppendable.toString()
        return GeneratorOutputs(ClassName(API_PACKAGE, className), outputs)
    }

    private fun TypeSpec.Builder.generateOptions(
        arguments: Collection<KotlinCompilerArgument>,
        argumentTypeName: ClassName,
        skipXX: Boolean,
    ) {
        val enumsToGenerate = mutableMapOf<KClass<*>, TypeSpec.Builder>()
        val enumsExperimental = mutableMapOf<KClass<*>, Boolean>()

        arguments.forEach { argument ->
            val name = argument.extractName()
            if (skipXX && name.startsWith("XX_")) return@forEach
            val experimental = name.startsWith("XX_") || name.startsWith("X_")

            if (argument.releaseVersionsMetadata.removedVersion != null) {
                return@forEach
            }

            val argumentTypeParameter =
                argument.valueType::class.supertypes.single { it.classifier == KotlinArgumentValueType::class }.arguments.first().type!!.let {
                    when (val type = it.classifier) {
                        is KClass<*> if type.isSubclassOf(Enum::class) && type in enumNameAccessors -> {
                            val enumConstants = type.java.enumConstants.filterIsInstance<Enum<*>>()
                            enumsToGenerate[type] = generateEnumTypeBuilder(enumConstants, type.accessor())
                            if (type !in enumsExperimental && experimental) {
                                enumsExperimental[type] = true
                            } else if (type in enumsExperimental && !experimental) {
                                // if at least one option that is NOT experimental uses the enum
                                // then the enum is not experimental itself
                                enumsExperimental[type] = false
                            }
                            ClassName("$API_PACKAGE.enums", type.simpleName!!)
                        }
                        else -> {
                            it.asTypeName()
                        }
                    }
                }.copy(nullable = argument.valueType.isNullable.current)
            property(name, argumentTypeName.parameterizedBy(argumentTypeParameter)) {
                annotation<JvmField>()
                // KT-28979 Need a way to escape /* in kdoc comments
                // inserting a zero-width space is not ideal, but we do actually have one compiler argument that breaks the KDoc without it
                addKdoc(argument.description.current.replace("/*", "/\u200B*").replace("*/", "*\u200B/"))
                if (experimental) {
                    addAnnotation(ANNOTATION_EXPERIMENTAL)
                    addKdoc("\n\nWARNING: this option is EXPERIMENTAL and it may be changed in the future without notice or may be removed entirely.")
                }
                initializer("%T(%S)", argumentTypeName, name)
            }
        }

        enumsToGenerate.forEach { (type, typeSpecBuilder) ->
            if (enumsExperimental.getOrDefault(type, false)) {
                typeSpecBuilder.addAnnotation(ANNOTATION_EXPERIMENTAL)
            }
            writeEnumFile(typeSpecBuilder.build(), type)
        }
    }

    fun generateEnumTypeBuilder(
        sourceEnum: Collection<Enum<*>>,
        nameAccessor: KProperty1<Any, String>,
    ): TypeSpec.Builder {
        val className = ClassName("$API_PACKAGE.enums", sourceEnum.first()::class.simpleName!!)
        return TypeSpec.enumBuilder(className).apply {
            property<String>("stringValue") {
                initializer("stringValue")
            }
            addKdoc(KDOC_SINCE_2_3_0)
            primaryConstructor(FunSpec.constructorBuilder().addParameter("stringValue", String::class).build())
            sourceEnum.forEach {
                addEnumConstant(
                    it.name.uppercase(),
                    TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%S", nameAccessor.get(it)).build()
                )
            }
        }
    }

    fun writeEnumFile(typeSpec: TypeSpec, sourceEnum: KClass<*>) {
        val className = ClassName("$API_PACKAGE.enums", sourceEnum.simpleName!!)
        val enumFileAppendable = createGeneratedFileAppendable()
        val enumFile = FileSpec.builder(className).apply {
            addType(typeSpec)
        }.build()
        enumFile.writeTo(enumFileAppendable)
        outputs += Path(enumFile.relativePath) to enumFileAppendable.toString()
    }


    fun TypeSpec.Builder.generateArgumentType(argumentsClassName: String): String {
        require(argumentsClassName.endsWith("Arguments"))
        val argumentTypeName = argumentsClassName.removeSuffix("s")
        val typeSpec = TypeSpec.Companion.classBuilder(argumentTypeName).apply {
            addKdoc(KDOC_BASE_OPTIONS_CLASS, ClassName(API_PACKAGE, argumentsClassName))
            addTypeVariable(TypeVariableName.Companion("V"))
            property<String>("id") {
                initializer("id")
            }
            primaryConstructor(FunSpec.Companion.constructorBuilder().addParameter("id", String::class).build())
        }.build()
        addType(typeSpec)
        return argumentTypeName
    }

    fun TypeSpec.Builder.generateGetPutFunctions(parameter: ClassName) {
        function("get") {
            addKdoc(KDOC_OPTIONS_GET)
            addModifiers(KModifier.ABSTRACT)
            val typeParameter = TypeVariableName("V")
            returns(typeParameter)
            addModifiers(KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
        }
        function("set") {
            addKdoc(KDOC_OPTIONS_SET)
            addModifiers(KModifier.ABSTRACT)
            val typeParameter = TypeVariableName("V")
            addModifiers(KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)
        }
    }
}

fun TypeSpec.Builder.generateArgumentType(argumentsClassName: String): String {
    require(argumentsClassName.endsWith("Arguments"))
    val argumentTypeName = argumentsClassName.removeSuffix("s")
    val typeSpec =
        TypeSpec.classBuilder(argumentTypeName).apply {
            addKdoc(KDOC_BASE_OPTIONS_CLASS, ClassName(API_PACKAGE, argumentsClassName))
            addTypeVariable(TypeVariableName("V"))
            property<String>("id") {
                initializer("id")
            }
            primaryConstructor(FunSpec.constructorBuilder().addParameter("id", String::class).build())
        }.build()
    addType(typeSpec)
    return argumentTypeName
}