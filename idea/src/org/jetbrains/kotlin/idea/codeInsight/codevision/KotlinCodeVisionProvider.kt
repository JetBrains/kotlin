/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.codevision

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinLanguage

@Suppress("UnstableApiUsage")
class KotlinCodeVisionProvider : InlayHintsProvider<KotlinCodeVisionProvider.KotlinCodeVisionSettings> {

    override val key: SettingsKey<KotlinCodeVisionSettings> = SettingsKey("CodeVision")
    override val name: String = KotlinBundle.message("hints.title.codevision")
    override val previewText: String? = null

    var usagesLimit: Int = 100
    var inheritorsLimit: Int = 100

    override fun isLanguageSupported(language: Language): Boolean = language is KotlinLanguage

    override fun createConfigurable(settings: KotlinCodeVisionSettings): ImmediateConfigurable = createImmediateConfigurable(settings)

    override fun createSettings(): KotlinCodeVisionSettings = KotlinCodeVisionSettings()

    override fun getCollectorFor(
        file: PsiFile, editor: Editor, settings: KotlinCodeVisionSettings, sink: InlayHintsSink
    ): InlayHintsCollector? {
        if (!settings.showUsages && !settings.showInheritors) return null
        return KotlinCodeVisionHintsCollector(editor, settings.showUsages, settings.showInheritors, usagesLimit, inheritorsLimit)
    }

    data class KotlinCodeVisionSettings(var showUsages: Boolean = false, var showInheritors: Boolean = false)
}