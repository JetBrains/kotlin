/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing

import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.TokenSet

interface KtxParsingExtension {
    companion object {
        val name = "org.jetbrains.kotlin.ktxParsingExtension";
        val extensionPointName: ExtensionPointName<KtxParsingExtension> = ExtensionPointName.create(name)

        fun registerExtensionPoint(project: Project) {
            Extensions.getArea(project).registerExtensionPoint(
                extensionPointName.name,
                KtxParsingExtension::class.java.name,
                ExtensionPoint.Kind.INTERFACE
            )
        }

        fun registerExtension(project: Project, extension: KtxParsingExtension) {

            if (!Extensions.getArea(project).hasExtensionPoint(extensionPointName.name)) {
                Extensions.getArea(project).registerExtensionPoint(
                    extensionPointName.name,
                    KtxParsingExtension::class.java.name,
                    ExtensionPoint.Kind.INTERFACE
                )
            }

            val extensionPoint = Extensions.getArea(project).getExtensionPoint(extensionPointName)
            if(extensionPoint.hasAnyExtensions())
                throw IllegalStateException("The extension point "+extensionPointName.name+" is already registered")

            Extensions.getArea(project).getExtensionPoint(extensionPointName).registerExtension(extension)
        }

        fun unregisterExtension(project: Project, extension: KtxParsingExtension) {

            val extensionPoint = Extensions.getArea(project).getExtensionPoint(extensionPointName)
            if(!extensionPoint.hasAnyExtensions())
                throw IllegalStateException("The extension point "+extensionPointName.name+" is not registered")

            extensionPoint.unregisterExtension(extension)
        }

        fun getInstance(project: Project): KtxParsingExtension {
            val projectArea = Extensions.getArea(project)
            if (!projectArea.hasExtensionPoint(extensionPointName.name)) return object : KtxParsingExtension {}
            val extensionPoint = projectArea.getExtensionPoint(extensionPointName)
            return extensionPoint.extensions.singleOrNull() ?: object : KtxParsingExtension {}
        }
    }

    fun atKtxStart(parser: KotlinExpressionParsing) = false
    fun createStatementsParser(parser: KotlinExpressionParsing, closeTagToken: String?) = object : KtxStatementsParser {}
    fun parseKtxTag(parser: KotlinExpressionParsing) {}
    fun parseKtxExpressionFollow(): TokenSet? = null
}

interface KtxStatementsParser {
    fun handleTag() {}
    fun finish() {}
    fun shouldBreak() = false
    fun shouldContinue() = false
}

