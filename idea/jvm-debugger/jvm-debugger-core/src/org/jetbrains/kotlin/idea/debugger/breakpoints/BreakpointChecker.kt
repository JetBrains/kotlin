/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.intellij.openapi.extensions.Extensions
import com.intellij.xdebugger.breakpoints.XBreakpointType
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import org.jetbrains.debugger.SourceInfo
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

class BreakpointChecker {
    private companion object {
        private val BREAKPOINT_TYPES = mapOf(
            KotlinLineBreakpointType::class.java to BreakpointType.Line,
            KotlinFieldBreakpointType::class.java to BreakpointType.Field,
            KotlinFunctionBreakpointType::class.java to BreakpointType.Function,
            JavaLineBreakpointType.LambdaJavaBreakpointVariant::class.java to BreakpointType.Lambda,
            KotlinLineBreakpointType.LineKotlinBreakpointVariant::class.java to BreakpointType.Line,
            KotlinLineBreakpointType.KotlinBreakpointVariant::class.java to BreakpointType.All
        )
    }

    enum class BreakpointType(val prefix: String) {
        Line("L"),
        Field("F"),
        Function("M"), // method
        Lambda("λ"),
        All("*") // line & lambda
    }

    @Suppress("SimplifiableCall")
    private val breakpointTypes: List<XLineBreakpointType<*>> = run {
        val extensionPoint = Extensions.getArea(null)
            .getExtensionPoint<XBreakpointType<*, *>>(XBreakpointType.EXTENSION_POINT_NAME.name)

        extensionPoint.extensions
            .filterIsInstance<XLineBreakpointType<*>>()
            .filter { it is KotlinBreakpointType }
    }

    fun check(file: KtFile, line: Int): EnumSet<BreakpointType> {
        val actualBreakpointTypes = EnumSet.noneOf(BreakpointType::class.java)

        for (breakpointType in breakpointTypes) {
            val sign = BREAKPOINT_TYPES[breakpointType.javaClass] ?: continue
            val isApplicable = breakpointType.canPutAt(file.virtualFile, line, file.project)

            if (breakpointType is KotlinLineBreakpointType) {
                if (isApplicable) {
                    val variants = breakpointType.computeVariants(file.project, SourceInfo(file.virtualFile, line))
                    if (variants.isNotEmpty()) {
                        actualBreakpointTypes += variants.mapNotNull { BREAKPOINT_TYPES[it.javaClass] }
                    } else {
                        actualBreakpointTypes += sign
                    }
                }
            } else if (isApplicable) {
                actualBreakpointTypes += sign
            }
        }

        return actualBreakpointTypes
    }
}