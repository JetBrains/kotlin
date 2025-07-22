/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.types.IntType
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.cli.arguments.generator.calculateName
import org.jetbrains.kotlin.cli.arguments.generator.levelToClassNameMap
import org.jetbrains.kotlin.generators.kotlinpoet.annotation
import org.jetbrains.kotlin.generators.kotlinpoet.function
import org.jetbrains.kotlin.generators.kotlinpoet.property
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.KClass

class BtaImplGenerator() : BtaGenerator {

    private val outputs = mutableListOf<Pair<Path, String>>()

    override fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: TypeName?, skipXX: Boolean): GeneratorOutputs {
        val apiClassName = level.name.capitalizeAsciiOnly()
        val className = apiClassName + "Impl"
        val mainFileAppendable = createGeneratedFileAppendable()
        val mainFile = FileSpec.Companion.builder(IMPL_PACKAGE, className).apply {
            addAnnotation(
                AnnotationSpec.Companion.builder(ClassName("kotlin", "OptIn"))
                    .addMember("%T::class", ANNOTATION_EXPERIMENTAL).build()
            )
            addType(
                TypeSpec.Companion.classBuilder(className).apply {
                    parentClass?.let { superclass(it) }
                    addSuperinterface(ClassName(API_PACKAGE, level.name.capitalizeAsciiOnly()))
                    if (level.nestedLevels.isNotEmpty()) {
                        addModifiers(KModifier.OPEN)
                    }
                    val converterFun = FunSpec.Companion.builder("toCompilerArguments").apply {
                        val compilerArgumentInfo = levelToClassNameMap.getValue(level.name)
                        val compilerArgumentsClass = ClassName(compilerArgumentInfo.classPackage, compilerArgumentInfo.className)
                        addParameter(
                            ParameterSpec.Companion.builder("arguments", compilerArgumentsClass).apply {
                                if (level.nestedLevels.isEmpty()) {
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

                    val argument = generateArgumentType(apiClassName)
                    val argumentTypeName = ClassName(API_PACKAGE, apiClassName, argument)
                    generateGetPutFunctions(argumentTypeName)
                    generateOptions(level.filterOutDroppedArguments(), apiClassName, converterFun, skipXX)
                    converterFun.addStatement("return arguments")
                    addFunction(converterFun.build())
                }.build()
            )
        }.build()
        mainFile.writeTo(mainFileAppendable)
        outputs += Path(mainFile.relativePath) to mainFileAppendable.toString()
        return GeneratorOutputs(ClassName(IMPL_PACKAGE, className), outputs)
    }

    private fun generateOptions(
        arguments: Collection<KotlinCompilerArgument>,
        apiClassName: String,
        converterFun: FunSpec.Builder,
        skipXX: Boolean,
    ) {
        arguments.forEach { argument ->
            val name = argument.extractName()
            if (skipXX && name.startsWith("XX_")) return@forEach

            if (argument.releaseVersionsMetadata.removedVersion != null) {
                return@forEach
            }

            val clazz = argument.valueType::class
                .supertypes.single { it.classifier == KotlinArgumentValueType::class }
                .arguments.first().type!!.classifier as KClass<*>

            val member = MemberName(ClassName(API_PACKAGE, apiClassName, "Companion"), name)
            when {
                clazz in enumNameAccessors -> converterFun.addStatement(
                    "if (%S in optionsMap) { arguments.%N = get(%M)${if (argument.valueType.isNullable.current) "?" else ""}.stringValue }",
                    name,
                    argument.calculateName(),
                    member
                )
                argument.valueType is IntType -> converterFun.addStatement(
                    "if (%S in optionsMap) { arguments.%N = get(%M)${if (argument.valueType.isNullable.current) "?" else ""}.toString() }",
                    name,
                    argument.calculateName(),
                    member
                )
                else -> converterFun.addStatement(
                    "if (%S in optionsMap) { arguments.%N = get(%M) }",
                    name,
                    argument.calculateName(),
                    member
                )
            }
        }
    }

    fun generateArgumentType(argumentsClassName: String): String {
        require(argumentsClassName.endsWith("Arguments"))
        val argumentTypeName = argumentsClassName.removeSuffix("s")
        return argumentTypeName
    }

    fun TypeSpec.Builder.generateGetPutFunctions(parameter: ClassName) {
        val mapProperty = property(
            "optionsMap",
            ClassName("kotlin.collections", "MutableMap").parameterizedBy(typeNameOf<String>(), ANY.copy(nullable = true))
        ) {
            addModifiers(KModifier.PRIVATE)
            initializer("%M()", MemberName("kotlin.collections", "mutableMapOf"))
        }
        function("get") {
            val typeParameter = TypeVariableName.Companion("V")
            annotation<Suppress> {
                addMember("%S", "UNCHECKED_CAST")
            }
            returns(typeParameter)
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addStatement("return %N[key.id] as %T", mapProperty, typeParameter)
        }
        function("set") {
            val typeParameter = TypeVariableName.Companion("V")
            addModifiers(KModifier.OVERRIDE, KModifier.OPERATOR)
            addTypeVariable(typeParameter)
            addParameter("key", parameter.parameterizedBy(typeParameter))
            addParameter("value", typeParameter)
            addStatement("%N[key.id] = %N", mapProperty, "value")
        }
    }
}