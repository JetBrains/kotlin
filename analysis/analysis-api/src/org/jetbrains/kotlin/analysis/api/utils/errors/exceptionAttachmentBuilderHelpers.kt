/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.utils.errors

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry as withPsiEntryWithoutKaModule

@KaAnalysisApiInternals
public fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?, moduleFactory: (PsiElement) -> KaModule) {
    withPsiEntry(name, psi, psi?.let(moduleFactory))
}

@KaAnalysisApiInternals
public fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?, module: KaModule?) {
    withPsiEntryWithoutKaModule(name, psi)
    withKaModuleEntry("${name}Module", module)
}

@KaAnalysisApiInternals
public fun ExceptionAttachmentBuilder.withKaModuleEntry(name: String, module: KaModule?) {
    withEntry(name, module) { module -> module.moduleDescription }
}

@KaAnalysisApiInternals
public fun ExceptionAttachmentBuilder.withClassEntry(name: String, element: Any?) {
    withEntry(name, element) { it::class.java.name }
}
