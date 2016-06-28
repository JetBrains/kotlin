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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.refactoring.rename.ResolveSnapshotProvider
import com.intellij.util.containers.HashMap
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.refactoring.explicateAsTextForReceiver
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

class KotlinResolveSnapshotProvider : ResolveSnapshotProvider() {
    override fun createSnapshot(scope: PsiElement) = object : ResolveSnapshot() {
        private val project = scope.project
        private val document = PsiDocumentManager.getInstance(project).getDocument(scope.containingFile)!!
        private val refExpressionToDescriptor = HashMap<SmartPsiElementPointer<*>, PropertyDescriptor>()

        init {
            scope.accept(
                    object: KtTreeVisitorVoid() {
                        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                            if (expression.getQualifiedExpressionForSelector() != null) return super.visitSimpleNameExpression(expression)
                            val context = expression.analyze(BodyResolveMode.PARTIAL)
                            val targetDescriptor = expression.mainReference.resolveToDescriptors(context).singleOrNull() ?: return
                            if (targetDescriptor !is PropertyDescriptor) return
                            refExpressionToDescriptor[expression.createSmartPointer()] = targetDescriptor
                        }
                    }
            )
        }

        override fun apply(name: String) {
            PsiDocumentManager.getInstance(project).commitDocument(document)

            val elementsToShorten = ArrayList<KtElement>()
            for ((refExprPointer, targetDescriptor) in refExpressionToDescriptor) {
                val refExpr = refExprPointer.element ?: continue
                if (refExpr.text != name) continue
                val containingDescriptor = targetDescriptor.containingDeclaration
                val qualifiedRefText = if (containingDescriptor is ClassDescriptor) {
                    "${containingDescriptor.explicateAsTextForReceiver()}.${targetDescriptor.name.asString()}"
                }
                else {
                    targetDescriptor.importableFqName?.asString() ?: continue
                }
                val qualifiedRefExpr = KtPsiFactory(project).createExpression(qualifiedRefText)
                elementsToShorten += refExpr.replaced(qualifiedRefExpr)
            }
            ShortenReferences { ShortenReferences.Options.ALL_ENABLED }.process(elementsToShorten)
        }
    }
}