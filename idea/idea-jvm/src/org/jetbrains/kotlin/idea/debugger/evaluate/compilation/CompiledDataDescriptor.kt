/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate.compilation

import com.intellij.debugger.SourcePosition
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.ClassToLoad

data class CompiledDataDescriptor(
    val classes: List<ClassToLoad>,
    val parameters: List<CodeFragmentParameter.Dumb>,
    val crossingBounds: Set<CodeFragmentParameter.Dumb>,
    val mainMethodSignature: CodeFragmentCompiler.MethodSignature,
    val sourcePosition: SourcePosition
) {
    companion object {
        fun from(result: CodeFragmentCompiler.CompilationResult, sourcePosition: SourcePosition): CompiledDataDescriptor {
            val localFunctionSuffixes = result.localFunctionSuffixes

            val dumbParameters = ArrayList<CodeFragmentParameter.Dumb>(result.parameterInfo.parameters.size)
            for (parameter in result.parameterInfo.parameters) {
                val dumb = parameter.dumb
                if (dumb.kind == CodeFragmentParameter.Kind.LOCAL_FUNCTION) {
                    val suffix = localFunctionSuffixes[dumb]
                    if (suffix != null) {
                        dumbParameters += dumb.copy(name = dumb.name + suffix)
                        continue
                    }
                }

                dumbParameters += dumb
            }

            return CompiledDataDescriptor(
                result.classes,
                dumbParameters,
                result.parameterInfo.crossingBounds,
                result.mainMethodSignature,
                sourcePosition
            )
        }
    }
}

val CompiledDataDescriptor.mainClass: ClassToLoad
    get() = classes.first { it.isMainClass }