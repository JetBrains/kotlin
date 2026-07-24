/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.compilation

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSpi
import org.jetbrains.kotlin.analysis.api.KaSpiExtensionPoint

/**
 * A handler which is called whenever a new class file is produced, when compiling sources to the JVM target.
 *
 * @see KaCompilationTarget.JVM
 */
@KaSpi
@KaExperimentalApi
public fun interface KaCompiledClassHandler {
    /**
     * [handleClassDefinition] is called whenever a new class file is produced.
     *
     * @param file The [PsiFile] containing the class definition. It can be `null` when the generated class file has no PSI file in sources,
     *  for example if it's an anonymous object from another module, regenerated during inlining.
     * @param className The name of the class in the JVM's internal name format, for example `"java/lang/Object"`.
     */
    @KaSpiExtensionPoint
    public fun handleClassDefinition(file: PsiFile?, className: String)
}
