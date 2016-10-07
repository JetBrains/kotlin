/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.generators.arguments

import com.sampullara.cli.Argument
import org.jetbrains.kotlin.cli.common.arguments.DefaultValues
import org.jetbrains.kotlin.cli.common.arguments.GradleOption
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import java.io.PrintStream
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KProperty1
import kotlin.reflect.memberProperties

// Additional properties that should be included in interface
@Suppress("unused")
interface AdditionalGradleProperties {
    @GradleOption(EmptyList::class)
    @Argument(description = "A list of additional compiler arguments")
    var freeCompilerArgs: List<String>

    object EmptyList : DefaultValues("emptyList()")
}

fun main(args: Array<String>) {
    val srcDir = File("libraries/tools/kotlin-gradle-plugin/src/main/kotlin")
    val additionalGradleOptions = gradleOptions<AdditionalGradleProperties>()

    // generate jvm interface
    val jvmInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions")
    val optionsFromK2JVMCompilerArguments = gradleOptions<K2JVMCompilerArguments>()
    File(srcDir, jvmInterfaceFqName).usePrinter {
        generateInterface(jvmInterfaceFqName,
                          optionsFromK2JVMCompilerArguments + additionalGradleOptions)
    }

    // generate jvm impl
    val k2JvmCompilerArgumentsFqName = FqName(K2JVMCompilerArguments::class.qualifiedName!!)
    val jvmImplFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsBase")
    File(srcDir, jvmImplFqName).usePrinter {
        generateImpl(jvmImplFqName,
                     jvmInterfaceFqName,
                     k2JvmCompilerArgumentsFqName,
                     optionsFromK2JVMCompilerArguments)
    }

    // generate js interface
    val jsInterfaceFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions")
    val optionsFromK2JSCompilerArguments = gradleOptions<K2JSCompilerArguments>()
    File(srcDir, jsInterfaceFqName).usePrinter {
        generateInterface(jsInterfaceFqName,
                          optionsFromK2JSCompilerArguments +
                          additionalGradleOptions)
    }

    val k2JsCompilerArgumentsFqName = FqName(K2JSCompilerArguments::class.qualifiedName!!)
    val jsImplFqName = FqName("org.jetbrains.kotlin.gradle.dsl.KotlinJsOptionsBase")
    File(srcDir, jsImplFqName).usePrinter {
        generateImpl(jsImplFqName,
                     jsInterfaceFqName,
                     k2JsCompilerArgumentsFqName,
                     optionsFromK2JSCompilerArguments)
    }
}

private inline fun <reified T : Any> gradleOptions(): List<KProperty1<T, *>> =
        T::class.memberProperties.filter { it.findAnnotation<GradleOption>() != null }.sortedBy { it.name }

private fun File(baseDir: File, fqName: FqName): File {
    val fileRelativePath = fqName.asString().replace(".", "/") + ".kt"
    return File(baseDir, fileRelativePath)
}

private inline fun File.usePrinter(fn: Printer.()->Unit) {
    if (!exists()) {
        parentFile.mkdirs()
        createNewFile()
    }
    PrintStream(outputStream()).use {
        val printer = Printer(it)
        printer.fn()
    }
}

private fun Printer.generateInterface(type: FqName, properties: List<KProperty1<*, *>>) {
    generateDeclaration("interface", type) {
        for (property in properties) {
            println()
            generateDoc(property)
            generatePropertyDeclaration(property)
        }
    }
}

private fun Printer.generateImpl(
        type: FqName,
        parentType: FqName,
        argsType: FqName,
        properties: List<KProperty1<*, *>>
) {
    generateDeclaration("internal abstract class", type, afterType = ": $parentType") {
        fun KProperty1<*, *>.backingField(): String = "${this.name}Field"

        for (property in properties) {
            println()
            val backingField = property.backingField()
            val backingFieldType = property.gradleReturnType + "?"
            println("private var $backingField: $backingFieldType = null")
            generatePropertyDeclaration(property, modifiers = "override")
            withIndent {
                println("get() = $backingField ?: ${property.gradleDefaultValue}")
                println("set(value) { $backingField = value }")
            }
        }

        println()
        println("open fun updateArguments(args: $argsType) {")
        withIndent {
            for (property in properties) {
                val backingField = property.backingField()
                println("$backingField?.let { args.${property.name} = it }")
            }
        }
        println("}")
    }

    println()
    println("internal fun $argsType.fillDefaultValues() {")
    withIndent {
        for (property in properties) {
            println("${property.name} = ${property.gradleDefaultValue}")
        }
    }
    println("}")
}

private fun Printer.generateDeclaration(
        modifiers: String,
        type: FqName,
        afterType: String? = null,
        generateBody: Printer.()->Unit
) {
    println("// DO NOT EDIT MANUALLY!")
    println("// Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt")
    if (!type.parent().isRoot) {
        println("package ${type.parent()}")
        println()
    }
    print("$modifiers ${type.shortName()} ")
    afterType?.let { print("$afterType ") }
    println("{")
    withIndent {
        generateBody()
    }
    println("}")
}

private fun Printer.generatePropertyDeclaration(property: KProperty1<*, *>, modifiers: String = "") {
    val returnType = property.gradleReturnType
    println("$modifiers var ${property.name}: $returnType")
}

private fun Printer.generateDoc(property: KProperty1<*, *>) {
    val description = property.findAnnotation<Argument>()!!.description
    val possibleValues = property.gradleValues.possibleValues
    val defaultValue = property.gradleDefaultValue

    println("/**")
    println(" * $description")
    if (possibleValues != null) {
        println(" * Possible values: ${possibleValues.joinToString()}")
    }
    println(" * Default value: $defaultValue")
    println(" */")
}

private inline fun Printer.withIndent(fn: Printer.()->Unit) {
    pushIndent()
    fn()
    popIndent()
}

private val KProperty1<*, *>.gradleValues: DefaultValues
        get() = findAnnotation<GradleOption>()!!.value.objectInstance!!

private val KProperty1<*, *>.gradleDefaultValue: String
        get() = gradleValues.defaultValue

private val KProperty1<*, *>.gradleReturnType: String
        get() {
            var type = returnType.toString().substringBeforeLast("!")
            if (gradleDefaultValue == "null") {
                type += "?"
            }
            return type
        }

private inline fun <reified T> KAnnotatedElement.findAnnotation(): T? =
        annotations.filterIsInstance<T>().firstOrNull()
