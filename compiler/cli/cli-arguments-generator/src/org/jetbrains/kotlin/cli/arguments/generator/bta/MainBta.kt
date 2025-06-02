import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import org.jetbrains.kotlin.cli.common.arguments.BtaOption
import org.jetbrains.kotlin.cli.common.arguments.converter
import org.jetbrains.kotlin.generators.kotlinpoet.annotation
import org.jetbrains.kotlin.generators.kotlinpoet.function
import org.jetbrains.kotlin.generators.kotlinpoet.property
import org.jetbrains.kotlin.generators.kotlinpoet.toNullable
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import java.nio.file.Paths
import kotlin.reflect.KClass

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun main() {
    val levels = mutableListOf<Pair<KotlinCompilerArgumentsLevel, TypeName?>>(kotlinCompilerArguments.topLevel to null)
    while (levels.isNotEmpty()) {
        val level = levels.popLast()
        val parentClass = generateArgumentsForLevel(level.first, level.second)
        levels += level.first.nestedLevels.map { it to parentClass }
    }
}

val BTA_PACKAGE = "org.jetbrains.kotlin.build.tools.api"

fun generateArgumentsForLevel(level: KotlinCompilerArgumentsLevel, parentClass: TypeName? = null): TypeName {
    val className = level.name.capitalizeAsciiOnly()
    val levelFile = Paths.get("compiler/cli/build/generated/bta")
    FileSpec.builder(BTA_PACKAGE, className).apply {
        addType(
            TypeSpec.classBuilder(className).apply {
                parentClass?.let { superclass(it) }
                val argument = generateArgumentType(className)
                val argumentTypeName = ClassName(BTA_PACKAGE, className, argument)
                generateGetPutFunctions(argumentTypeName)
                addType(TypeSpec.companionObjectBuilder().apply {
                    generateOptions(level.arguments, argumentTypeName)
                }.build())
            }.build()
        )
    }.build().writeTo(levelFile)
    return ClassName(BTA_PACKAGE, className)
}

private fun TypeSpec.Builder.generateOptions(arguments: Set<KotlinCompilerArgument>, argumentTypeName: ClassName) {
    arguments.forEach { argument ->
        val experimental: Boolean
        val name = argument.name.uppercase().replace("-", "_").let {
            if (it.startsWith("X") && it != "X") {
                experimental = true
                it.removePrefix("X")
            } else {
                experimental = false
                it
            }
        }

        val argumentTypeParameter = (argument.additionalAnnotations.filterIsInstance<BtaOption>().singleOrNull()?.let { btaOption ->
            btaOption.stringTypeHint.converter?.let { converter ->
                (converter::class.supertypes.first().arguments.first().type!!.classifier as KClass<*>).asTypeName()
            }
        } ?: argument.valueType::class
            .supertypes.single { it.classifier == KotlinArgumentValueType::class }
            .arguments.first().type!!.asTypeName()
                ).copy(nullable = argument.valueType.isNullable.current)
        property(name, argumentTypeName.parameterizedBy(argumentTypeParameter)) {
            annotation<JvmField>()
            addKdoc(argument.description.current)
            if (experimental) {
                annotation<Deprecated>() // TODO deprecated only as placeholder, should be experimental opt in or something like that, or not generated at all
            }
            initializer("%T(%S)", argumentTypeName, name)
        }
    }
}

fun TypeSpec.Builder.generateArgumentType(argumentsClassName: String): String {
    require(argumentsClassName.endsWith("Arguments"))
    val argumentTypeName = argumentsClassName.removeSuffix("s")
    val typeSpec =
        TypeSpec.classBuilder(argumentTypeName).apply {
            addTypeVariable(TypeVariableName("V"))
            property<String>("id") {
                initializer("id")
            }
            primaryConstructor(FunSpec.constructorBuilder().addParameter("id", String::class).build())
        }.build()
    addType(typeSpec)
    return argumentTypeName
}

fun TypeSpec.Builder.generateGetPutFunctions(parameter: ClassName) {
//    public operator fun <V> get(key: BaseCompilerArgument<V>): V?
//    public operator fun <V> set(key: BaseCompilerArgument<V>, value: V)
    function("get") {
        val typeParameter = TypeVariableName("V")
        returns(typeParameter.toNullable())
        addModifiers(KModifier.OPERATOR)
        addTypeVariable(typeParameter)
        addParameter("key", parameter.parameterizedBy(typeParameter))
    }
    function("set") {
        val typeParameter = TypeVariableName("V")
        addModifiers(KModifier.OPERATOR)
        addTypeVariable(typeParameter)
        addParameter("key", parameter.parameterizedBy(typeParameter))
        addParameter("value", typeParameter)
    }
}

