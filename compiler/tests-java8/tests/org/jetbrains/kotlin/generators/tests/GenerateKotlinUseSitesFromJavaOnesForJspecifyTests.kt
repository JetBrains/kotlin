/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.checkers.JSPECIFY_NULLABLE_ANNOTATION
import org.jetbrains.kotlin.checkers.JSPECIFY_NULLNESS_MISMATCH_MARK
import org.jetbrains.kotlin.checkers.JSPECIFY_NULLNESS_UNSPECIFIED_ANNOTATION
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.io.File
import java.lang.Exception

private data class ParsedMethod(
    val returnType: String,
    val funName: String,
    val typeParameters: String,
    val valueParameters: String,
    val body: String
)

private data class ParsedCallComponent(
    val callee: String,
    val typeArguments: String?,
    val valueArguments: String?
)

private data class ParsedCall(
    val jspecifyMark: String?,
    val receiver: String,
    val callComponents: Sequence<ParsedCallComponent>,
    val indent: String
)

class GenerateKotlinUseSitesFromJavaOnesForJspecifyTests {
    companion object {
        private const val TESTS_DIRECTORY = "compiler/testData/foreignAnnotationsJava8/tests/jspecify/java"

        private const val JSPECIFY_ANNOTATIONS = """(?:$JSPECIFY_NULLABLE_ANNOTATION|$JSPECIFY_NULLNESS_UNSPECIFIED_ANNOTATION)"""
        private const val JSPECIFY_MARK = """(?:\/\/ $JSPECIFY_NULLNESS_MISMATCH_MARK)\n\s*"""
        private const val TYPE_COMPONENT = """(?:$JSPECIFY_ANNOTATIONS )?(?:\w+)(?:\.\w+)*"""
        private const val TYPE_ARGUMENTS = """<(?:$TYPE_COMPONENT)(?:\s*,\s*$TYPE_COMPONENT)*>"""
        private const val TYPE_PARAMETER = """\w(?: (?:extends|super) $TYPE_COMPONENT)?"""
        private const val TYPE_PARAMETERS = """<(?:$TYPE_PARAMETER)(?:\s*,\s*$TYPE_PARAMETER)*>\s*"""
        private const val TYPE = """$TYPE_COMPONENT(?:$TYPE_ARGUMENTS)?"""
        private const val VALUE_PARAMETER = """\s*(?:$TYPE) (?:\w+)"""
        private const val VALUE_PARAMETERS = """\((?:$VALUE_PARAMETER)(?:\s*,\s*$VALUE_PARAMETER)*\s*\)"""
        private const val VALUE_ARGUMENT = """\w+"""
        private const val VALUE_ARGUMENTS = """\((?:(?:$VALUE_ARGUMENT)(?:\s*,\s*$VALUE_ARGUMENT)*\s*)?\)"""
        private const val CALL_COMPONENT = """\.($TYPE_ARGUMENTS)?(?:\w+)(?:$VALUE_ARGUMENTS)?"""
        private const val CALL = """\s*(?:$JSPECIFY_MARK)?(?:\w+)(?:(?:$CALL_COMPONENT)+);(\n+)\s*"""
        private const val FUN_BODY = """\{((?:$CALL)+)\s*}"""

        val methodRegex = Regex("""(?:public )?($TYPE_PARAMETERS)?(void|$TYPE) (\w+)($VALUE_PARAMETERS)? $FUN_BODY""")
        val valueParameterRegex = Regex("""($TYPE) (\w+)(,|\s*$)""")
        val callRegex = Regex(CALL.replace("?:", ""))
        val callComponentRegex = Regex(CALL_COMPONENT.replace("?:", ""))
    }

    private fun parseMethod(sourceCode: String): ParsedMethod? {
        val parsedResult = methodRegex.find(sourceCode) ?: return null
        val (typeParameters, returnType, funName, arguments, body) = parsedResult.destructured

        return ParsedMethod(
            transformTypesByAnnotationsIfNeeded(returnType),
            funName,
            typeParameters,
            arguments.removeSurrounding("(", ")"),
            body
        )
    }

    private fun parseCalls(funBody: String) =
        callRegex.findAll(funBody).map { call ->
            val jspecifyMark = call.groups[2]?.value
            val receiver = call.groups[3]?.value ?: throw Exception("Unable to parse receiver of $call")
            val callComponents = call.groups[4] ?: throw Exception("Unable to parse call components of $call")
            val indent = call.groups[22]?.value ?: throw Exception("Unable to parse indent of $call")
            val parsedCallComponents = callComponentRegex.findAll(callComponents.value).map { callComponent ->
                val calle = callComponent.groups[12]?.value
                    ?: throw Exception("Unable to parse calle of $callComponent")
                val typeArguments = callComponent.groups[1]?.value
                val valueArguments = callComponent.groups[13]?.value?.removeSurrounding("(", ")")
                    ?.let { transformTypesByAnnotationsIfNeeded(it) }
                ParsedCallComponent(calle, typeArguments, valueArguments)
            }
            ParsedCall(jspecifyMark, receiver, parsedCallComponents, indent)
        }

    private fun transformTypesByAnnotationsIfNeeded(valueArguments: String) =
        valueArguments
            .replace(Regex("""$JSPECIFY_NULLABLE_ANNOTATION (\w+)"""), "$1?")
            .replace(Regex("""$JSPECIFY_NULLNESS_UNSPECIFIED_ANNOTATION (\w+)"""), "$1")
            .replace("Object", "Any")
            .replace("void", "Unit")

    private fun generateKotlinCode(parsedMethod: ParsedMethod, parsedCalls: Sequence<ParsedCall>) = buildString {
        val methodValueParameters =
            transformTypesByAnnotationsIfNeeded(parsedMethod.valueParameters.replace(valueParameterRegex, "$2: $1$3"))
                .removeSuffixIfPresent(" ".repeat(4))
        val methodTypeParameters = parsedMethod.typeParameters.replace(" extends ", " : ")

        appendLine("fun $methodTypeParameters${parsedMethod.funName}($methodValueParameters): ${parsedMethod.returnType} {")

        for (call in parsedCalls) {
            if (call.jspecifyMark != null) {
                appendLine(" ".repeat(4) + call.jspecifyMark)
            }
            append(" ".repeat(4) + call.receiver)
            for (callComponent in call.callComponents) {
                val typeArguments = callComponent.typeArguments?.let { transformTypesByAnnotationsIfNeeded(it) } ?: ""
                val valueArguments = callComponent.valueArguments?.let { "($it)" } ?: ""
                append(".${callComponent.callee}${typeArguments}$valueArguments")
            }
            append(call.indent)
        }

        append("}")
    }

    private fun writeKotlinCode(javaFile: File, mode: String, code: String, modeDirective: String = "") {
        File("${javaFile.parentFile.parent}/kotlin/$mode/${javaFile.nameWithoutExtension}.kt")
            .writeText("// JAVA_SOURCES: ${javaFile.name}\n$modeDirective\n$code")
    }

    fun main() {
        val javaFiles = File(TESTS_DIRECTORY).walkTopDown()

        for (file in javaFiles) {
            if (!file.isFile || file.extension != "java") continue

            val parsedMethod = parseMethod(file.readText()) ?: continue
            val parsedCalls = parseCalls(parsedMethod.body)
            val generatedKotlinCode = generateKotlinCode(parsedMethod, parsedCalls)

            writeKotlinCode(file, "strictMode", generatedKotlinCode)
            writeKotlinCode(file, "warnMode", generatedKotlinCode, "// JSPECIFY_STATE warn\n")
        }
    }
}

fun main() = GenerateKotlinUseSitesFromJavaOnesForJspecifyTests().main()
