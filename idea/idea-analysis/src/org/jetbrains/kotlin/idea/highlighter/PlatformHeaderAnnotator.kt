/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.idea.caches.resolve.ModuleProductionSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.ModuleTestSourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.checkers.HeaderImplDeclarationChecker
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.diagnostics.SimpleDiagnostics

val ModuleDescriptor.sourceKind: SourceKind
    get() = when (getCapability(ModuleInfo.Capability)) {
        is ModuleProductionSourceInfo -> SourceKind.PRODUCTION
        is ModuleTestSourceInfo -> SourceKind.TEST
        else -> SourceKind.OTHER
    }

enum class SourceKind { OTHER, PRODUCTION, TEST }

val ModuleDescriptor.allImplementingCompatibleModules
    get() = allImplementingModules.filter {
        sourceKind == SourceKind.OTHER ||
        it.sourceKind == SourceKind.OTHER ||
        it.sourceKind == sourceKind
    }

class PlatformHeaderAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val declaration = element as? KtDeclaration ?: return
        if (!declaration.hasModifier(KtTokens.HEADER_KEYWORD)) return

        if (TargetPlatformDetector.getPlatform(declaration.containingKtFile) !is TargetPlatform.Default) return

        val defaultModuleDescriptor = declaration.findModuleDescriptor()
        val dependentDescriptors = defaultModuleDescriptor.allImplementingCompatibleModules
        if (dependentDescriptors.isEmpty()) return

        val diagnostics = validate(declaration, dependentDescriptors)
        KotlinPsiChecker().annotateElement(declaration, holder, diagnostics)
    }

    fun validate(declaration: KtDeclaration, modulesToCheck: Collection<ModuleDescriptor>): Diagnostics {
        val descriptor = declaration.toDescriptor() as? MemberDescriptor ?: return Diagnostics.EMPTY
        if (!descriptor.isHeader) return Diagnostics.EMPTY

        val diagnosticList = mutableListOf<Diagnostic>()
        val diagnosticSink = object : DiagnosticSink {
            override fun report(diagnostic: Diagnostic) {
                diagnosticList += diagnostic
            }

            override fun wantsDiagnostics() = true
        }
        for (module in modulesToCheck) {
            HeaderImplDeclarationChecker.checkHeaderDeclarationHasImplementation(
                    declaration, descriptor, diagnosticSink, module, checkImpl = false
            )
        }

        val suppressionCache = KotlinCacheService.getInstance(declaration.project).getSuppressionCache()
        val filteredList = diagnosticList.filter {
            !suppressionCache.isSuppressed(declaration, it.factory.name, it.severity)
        }
        return if (filteredList.isNotEmpty()) SimpleDiagnostics(filteredList) else Diagnostics.EMPTY
    }
}