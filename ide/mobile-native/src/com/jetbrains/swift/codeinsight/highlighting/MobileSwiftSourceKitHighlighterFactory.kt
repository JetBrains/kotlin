package com.jetbrains.swift.codeinsight.highlighting

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.swift.psi.SwiftFile

class MobileSwiftSourceKitHighlighterFactory : TextEditorHighlightingPassFactory, DumbAware, TextEditorHighlightingPassFactoryRegistrar {
    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        if (file is SwiftFile) {
            return SwiftSourceKitHighlightingPass(file.project, editor.document, false)
        }
        return null
    }

    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        registrar.registerTextEditorHighlightingPass(this, TextEditorHighlightingPassRegistrar.Anchor.FIRST, -1, false, false)
    }
}