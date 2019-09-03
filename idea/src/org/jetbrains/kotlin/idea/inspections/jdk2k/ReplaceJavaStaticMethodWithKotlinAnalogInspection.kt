/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.jdk2k

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.idea.util.textRangeIn
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReplaceJavaStaticMethodWithKotlinAnalogInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = callExpressionVisitor(fun(call) {
        val callee = call.calleeExpression ?: return
        val replacements = REPLACEMENTS[callee.text]
            ?.filter { it.filter(call) && it.transformation.isApplicable(call) }
            ?.takeIf { it.isNotEmpty() }
            ?.let { list ->
                val context = call.analyze(BodyResolveMode.PARTIAL)
                val callDescriptor = call.getResolvedCall(context) ?: return
                list.filter {
                    callDescriptor.isCalling(FqName(it.javaMethodFqName)) && it.transformation.isApplicableInContext(
                        call,
                        context
                    )
                }
            }
            ?.takeIf { it.isNotEmpty() }
            ?.map(::ReplaceWithKotlinAnalogFunction)
            ?.toTypedArray() ?: return

        holder.registerProblem(
            call,
            callee.textRangeIn(call),
            "Should be replaced with Kotlin function",
            *replacements
        )
    })

    private class ReplaceWithKotlinAnalogFunction(private val replacement: Replacement) : LocalQuickFix {
        override fun getName() = "Replace with `${replacement.kotlinFunctionShortName}` function"

        override fun getFamilyName() = "Replace with Kotlin analog"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val callExpression = descriptor.psiElement as? KtCallExpression ?: return
            replacement.transformation(callExpression, replacement)
        }
    }

    companion object {
        private val JAVA_PRIMITIVES = listOf(
            "Integer" to "Int",
            "Long" to "Long",
            "Byte" to "Byte",
            "Character" to "Char",
            "Short" to "Short",
            "Double" to "Double",
            "Float" to "Float"
        ).flatMap { (javaPrimitive, kotlinPrimitive) ->
            listOf(
                Replacement("java.lang.$javaPrimitive.toString", "kotlin.text.toString", ToExtensionFunctionWithNonNullableReceiver) {
                    it.valueArguments.size == 2
                },
                Replacement(
                    "java.lang.$javaPrimitive.toString",
                    "kotlin.primitives.$kotlinPrimitive.toString",
                    ToExtensionFunctionWithNullableReceiver
                ) {
                    it.valueArguments.size == 1
                },
                Replacement(
                    "java.lang.$javaPrimitive.compare",
                    "kotlin.primitives.$kotlinPrimitive.compareTo",
                    ToExtensionFunctionWithNonNullableReceiver
                )
            )
        }

        private val JAVA_IO = listOf(
            Replacement("java.io.PrintStream.print", "kotlin.io.print", filter = ::isJavaSystemOut),
            Replacement("java.io.PrintStream.println", "kotlin.io.println", filter = ::isJavaSystemOut)
        )

        // TODO: implement [java.lang.System.arraycopy]
        private val JAVA_SYSTEM = listOf(
            Replacement("java.lang.System.exit", "kotlin.system.exitProcess")
        )

        private val JAVA_MATH = listOf(
            Replacement("java.lang.Math.abs", "kotlin.math.abs"),
            Replacement("java.lang.Math.acos", "kotlin.math.acos"),
            Replacement("java.lang.Math.asin", "kotlin.math.asin"),
            Replacement("java.lang.Math.atan", "kotlin.math.atan"),
            Replacement("java.lang.Math.atan2", "kotlin.math.atan2"),
            Replacement("java.lang.Math.ceil", "kotlin.math.ceil"),
            Replacement("java.lang.Math.cos", "kotlin.math.cos"),
            Replacement("java.lang.Math.cosh", "kotlin.math.cosh"),
            Replacement("java.lang.Math.exp", "kotlin.math.exp"),
            Replacement("java.lang.Math.expm1", "kotlin.math.expm1"),
            Replacement("java.lang.Math.floor", "kotlin.math.floor"),
            Replacement("java.lang.Math.hypot", "kotlin.math.hypot"),
            Replacement("java.lang.Math.IEEEremainder", "kotlin.math.IEEErem", ToExtensionFunctionWithNonNullableReceiver),
            Replacement("java.lang.Math.log", "kotlin.math.ln"),
            Replacement("java.lang.Math.log1p", "kotlin.math.ln1p"),
            Replacement("java.lang.Math.log10", "kotlin.math.log10"),
            Replacement("java.lang.Math.max", "kotlin.math.max"),
            Replacement("java.lang.Math.max", "kotlin.ranges.coerceAtLeast", ToExtensionFunctionWithNonNullableReceiver),
            Replacement("java.lang.Math.min", "kotlin.math.min"),
            Replacement("java.lang.Math.min", "kotlin.ranges.coerceAtMost", ToExtensionFunctionWithNonNullableReceiver),
            Replacement("java.lang.Math.nextDown", "kotlin.math.nextDown", ToExtensionFunctionWithNonNullableReceiver),
            Replacement("java.lang.Math.nextAfter", "kotlin.math.nextTowards", ToExtensionFunctionWithNonNullableReceiver),
            Replacement("java.lang.Math.nextUp", "kotlin.math.nextUp", ToExtensionFunctionWithNonNullableReceiver),
            Replacement("java.lang.Math.pow", "kotlin.math.pow", ToExtensionFunctionWithNonNullableReceiver),
            Replacement("java.lang.Math.rint", "kotlin.math.round"),
            Replacement("java.lang.Math.round", "kotlin.math.roundToLong", ToExtensionFunctionWithNonNullableReceiver),
            Replacement("java.lang.Math.round", "kotlin.math.roundToInt", ToExtensionFunctionWithNonNullableReceiver),
            Replacement("java.lang.Math.signum", "kotlin.math.sign"),
            Replacement("java.lang.Math.sin", "kotlin.math.sin"),
            Replacement("java.lang.Math.sinh", "kotlin.math.sinh"),
            Replacement("java.lang.Math.sqrt", "kotlin.math.sqrt"),
            Replacement("java.lang.Math.tan", "kotlin.math.tan"),
            Replacement("java.lang.Math.tanh", "kotlin.math.tanh"),
            Replacement("java.lang.Math.copySign", "kotlin.math.withSign", ToExtensionFunctionWithNonNullableReceiver)
        )

        // TODO: implement [java.util.Arrays.binarySearch, java.util.Arrays.fill, java.util.Arrays.sort]
        private val JAVA_COLLECTIONS = listOf(
            Replacement("java.util.Arrays.copyOf", "kotlin.collections.copyOf", ToExtensionFunctionWithNonNullableReceiver) {
                it.valueArguments.size == 2
            },
            Replacement("java.util.Arrays.copyOfRange", "kotlin.collections.copyOfRange", ToExtensionFunctionWithNonNullableReceiver),
            Replacement("java.util.Arrays.equals", "kotlin.collections.contentEquals", ToExtensionFunctionWithNonNullableArguments),
            Replacement("java.util.Arrays.deepEquals", "kotlin.collections.contentDeepEquals", ToExtensionFunctionWithNonNullableArguments),
            Replacement(
                "java.util.Arrays.deepHashCode",
                "kotlin.collections.contentDeepHashCode",
                ToExtensionFunctionWithNonNullableReceiver
            ),
            Replacement("java.util.Arrays.hashCode", "kotlin.collections.contentHashCode", ToExtensionFunctionWithNonNullableReceiver),
            Replacement(
                "java.util.Arrays.deepToString",
                "kotlin.collections.contentDeepToString",
                ToExtensionFunctionWithNonNullableReceiver
            ),
            Replacement("java.util.Arrays.toString", "kotlin.collections.contentToString", ToExtensionFunctionWithNonNullableReceiver),
            Replacement("java.util.Arrays.asList", "kotlin.collections.listOf"),
            Replacement("java.util.Arrays.asList", "kotlin.collections.mutableListOf"),
            Replacement("java.util.Set.of", "kotlin.collections.setOf"),
            Replacement("java.util.Set.of", "kotlin.collections.mutableSetOf"),
            Replacement("java.util.List.of", "kotlin.collections.listOf"),
            Replacement("java.util.List.of", "kotlin.collections.mutableListOf")
        )

        private val REPLACEMENTS = (JAVA_MATH + JAVA_SYSTEM + JAVA_IO + JAVA_PRIMITIVES + JAVA_COLLECTIONS)
            .groupBy { it.javaMethodShortName }
    }
}

data class Replacement(
    val javaMethodFqName: String,
    val kotlinFunctionFqName: String,
    val transformation: Transformation = WithoutAdditionalTransformation,
    val filter: (KtCallExpression) -> Boolean = { true }
) {
    private fun String.shortName() = takeLastWhile { it != '.' }

    val javaMethodShortName = javaMethodFqName.shortName()

    val kotlinFunctionShortName = kotlinFunctionFqName.shortName()
}

private fun isJavaSystemOut(callExpression: KtCallExpression): Boolean =
    (callExpression.calleeExpression as? KtSimpleNameExpression)
        ?.getReceiverExpression()
        ?.resolveToCall()
        ?.isCalling(FqName("java.lang.System.out")) ?: false