/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreProjectEnvironment
import com.intellij.lang.MetaLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.parsing.KotlinParserDefinition

internal data class PsiSetup(
    val applicationEnvironment: CoreApplicationEnvironment,
    val projectEnvironment: CoreProjectEnvironment,
    val project: Project,
    val disposable: Disposable
)

internal fun setup(): PsiSetup {
    val disposable = Disposer.newDisposable()

    val applicationEnvironment = CoreApplicationEnvironment(disposable, false)

    val projectEnvironment = CoreProjectEnvironment(disposable, applicationEnvironment)

    CoreApplicationEnvironment.registerExtensionPoint(Extensions.getRootArea(), MetaLanguage.EP_NAME, MetaLanguage::class.java)


    applicationEnvironment.registerFileType(KotlinFileType.INSTANCE, "kt")
    applicationEnvironment.registerParserDefinition(KotlinParserDefinition())


    val project = projectEnvironment.project
    return PsiSetup(applicationEnvironment, projectEnvironment, project, disposable)
}

internal inline fun <T> withPsiSetup(l: PsiSetup.() -> T): T {
    val setup = setup()
    val t = setup.l()
    Disposer.dispose(setup.disposable)
    return t
}

