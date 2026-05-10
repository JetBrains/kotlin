/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry as withPsiEntryWithoutKaModule

@KaImplementationDetail
fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?, moduleFactory: (PsiElement) -> KaModule) {
    withPsiEntry(name, psi, psi?.let(moduleFactory))
}

@KaImplementationDetail
fun ExceptionAttachmentBuilder.withPsiEntry(name: String, psi: PsiElement?, module: KaModule?) {
    withPsiEntryWithoutKaModule(name, psi)
    withKaModuleEntry("${name}Module", module)
}

@OptIn(KaExperimentalApi::class, KaPlatformInterface::class)
@KaImplementationDetail
fun ExceptionAttachmentBuilder.withKaModuleEntry(name: String, module: KaModule?) {
    withEntry(name, module) { module -> module.moduleDescription }
    if (module is KaDanglingFileModule) {
        withKaModuleEntry("${name}contextModule", module.contextModule)
    }
}

@KaImplementationDetail
fun ExceptionAttachmentBuilder.withClassEntry(name: String, element: Any?) {
    withEntry(name, element) { it::class.java.name }
}
