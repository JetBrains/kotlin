/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.codevision

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinLanguage

@Suppress("UnstableApiUsage")
class KotlinCodeVisionProvider : InlayHintsProvider<NoSettings> {

    override val key: SettingsKey<NoSettings> = SettingsKey("CodeVision")
    override val name: String = KotlinBundle.message("hints.title.codevision")
    override val previewText: String? = null

    var usagesLimit: Int = 100
    var inheritorsLimit: Int = 100

    override fun isLanguageSupported(language: Language): Boolean = language is KotlinLanguage

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable = createImmediateConfigurable()

    override fun createSettings(): NoSettings = NoSettings()

    override fun getCollectorFor(
        file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink
    ): InlayHintsCollector? {

        val showUsages = Registry.`is`("kotlin.code-vision.usages", false)
        val showInheritors = Registry.`is`("kotlin.code-vision.inheritors", false)

        if (!showUsages && !showInheritors) return null

        return KotlinCodeVisionHintsCollector(editor, showUsages, showInheritors, usagesLimit, inheritorsLimit)
    }
}