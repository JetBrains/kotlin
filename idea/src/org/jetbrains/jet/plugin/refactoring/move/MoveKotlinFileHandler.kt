/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.move

import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.psi.PsiCompiledElement
import org.jetbrains.jet.lang.psi.JetFile
import java.util.ArrayList
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.util.MoveRenameUsageInfo
import org.jetbrains.jet.lang.psi.JetClassOrObject
import com.intellij.refactoring.util.TextOccurrencesUtil
import org.jetbrains.jet.plugin.search.usagesSearch.descriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiReference
import org.jetbrains.jet.lang.resolve.name.FqName
import java.util.Collections
import org.jetbrains.jet.plugin.references.JetSimpleNameReference
import com.intellij.openapi.util.Key
import org.jetbrains.jet.plugin.refactoring.getAndRemoveCopyableUserData
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.psiUtil.getPackage
import org.jetbrains.jet.plugin.codeInsight.JetFileReferencesResolver
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.plugin.references.JetReference
import org.jetbrains.jet.asJava.toLightElements
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetPsiUtil
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.diagnostic.Logger

public class MoveKotlinFileHandler : MoveFileHandler() {
    class object {
        private val LOG = Logger.getInstance(javaClass<MoveKotlinFileHandler>())

        class MoveRenameKotlinUsageInfo(
                reference: JetSimpleNameReference,
                range: TextRange,
                referencedElement: JetDeclaration,
                val newFqName: FqName
        ) : MoveRenameUsageInfo(reference.getElement(), reference, range.getStartOffset(), range.getEndOffset(), referencedElement, false) {
            val jetReference: JetSimpleNameReference get() = getReference() as JetSimpleNameReference
        }

        class MoveRenameJavaUsageInfo(
                reference: PsiReference,
                range: TextRange,
                referencedElement: JetDeclaration,
                val lightElementIndex: Int
        ) : MoveRenameUsageInfo(reference.getElement(), reference, range.getStartOffset(), range.getEndOffset(), referencedElement, false) {
            val jetDeclaration: JetDeclaration get() = getReferencedElement() as JetDeclaration
        }

        class PackageNameInfo(val oldPackageName: FqName, val newPackageName: FqName)

        private val PACKAGE_NAME_INFO_KEY =
                Key.create<PackageNameInfo>("${javaClass<MoveKotlinFileHandler>().getCanonicalName()}.PACKAGE_NAME_INFO_KEY")
    }

    private fun JetFile.packageMatchesDirectory(): Boolean {
        return getPackageName() == getParent()?.getPackage()?.getQualifiedName()
    }

    override fun canProcessElement(element: PsiFile?): Boolean {
        if (element is PsiCompiledElement || element !is JetFile) return false
        return !JavaProjectRootsUtil.isOutsideJavaSourceRoot(element)
    }

    override fun prepareMovedFile(file: PsiFile, moveDestination: PsiDirectory, oldToNewMap: Map<PsiElement, PsiElement>) {
        if (file is JetFile && file.packageMatchesDirectory()) {
            val newPackage = moveDestination.getPackage()
            if (newPackage != null) {
                file.putCopyableUserData(
                        PACKAGE_NAME_INFO_KEY,
                        PackageNameInfo(JetPsiUtil.getFQName(file), FqName(newPackage.getQualifiedName()))
                )
            }
        }
    }

    override fun findUsages(
            psiFile: PsiFile,
            newParent: PsiDirectory,
            searchInComments: Boolean,
            searchInNonJavaFiles: Boolean
    ): List<UsageInfo>? {
        if (psiFile !is JetFile || !psiFile.packageMatchesDirectory()) return null

        val project = psiFile.getProject()
        val newPackageName = newParent.getPackage()?.getQualifiedName() ?: ""

        return psiFile.getDeclarations().flatMap { declaration ->
            val name = (declaration as? JetNamedDeclaration)?.getName()
            if (name != null) {
                val newFqName = StringUtil.getQualifiedName(newPackageName, name)!!

                val results = ReferencesSearch.search(declaration, GlobalSearchScope.projectScope(project), false)
                        .toSet()
                        .mapTo(ArrayList<UsageInfo?>()) { ref ->
                            val range = ref.getRangeInElement()!!

                            when (ref) {
                                is JetSimpleNameReference ->
                                    MoveRenameKotlinUsageInfo(ref, range, declaration, FqName(newFqName))
                                is JetReference ->
                                    // Can't rebind other JetReferences
                                    null
                                else -> {
                                    val lightElementWithIndex =
                                            declaration.toLightElements().withIndices().find { p -> ref.isReferenceTo(p.second) }
                                    if (lightElementWithIndex != null && lightElementWithIndex.first >= 0) {
                                        MoveRenameJavaUsageInfo(ref, range, declaration, lightElementWithIndex.first)
                                    }
                                    else null
                                }
                            }
                        }
                        .filterNotNull()
                if (declaration is JetClassOrObject) {
                    declaration.descriptor?.let { DescriptorUtils.getFqName(it).asString() }?.let { stringToSearch ->
                        TextOccurrencesUtil.findNonCodeUsages(
                                declaration, stringToSearch, searchInComments, searchInNonJavaFiles, newFqName, results
                        )
                    }
                }

                results
            }
            else Collections.emptyList<UsageInfo>()
        }
    }

    override fun retargetUsages(usageInfos: List<UsageInfo>?, oldToNewMap: Map<PsiElement, PsiElement>?) {
        usageInfos?.forEach { usage ->
            when (usage) {
                is MoveRenameKotlinUsageInfo -> {
                    usage.jetReference.bindToFqName(usage.newFqName, false)
                }
                is MoveRenameJavaUsageInfo -> {
                    val lightElements = usage.jetDeclaration.toLightElements()
                    val index = usage.lightElementIndex
                    if (index < lightElements.size) {
                        usage.getReference()!!.bindToElement(lightElements[index])
                    }
                    else {
                        LOG.error("Can't find light element with index $index for ${usage.jetDeclaration.getText()} ")
                    }
                }
            }
        }
    }

    override fun updateMovedFile(file: PsiFile) {
        if (file !is JetFile) return

        val packageNameInfo = file.getAndRemoveCopyableUserData(PACKAGE_NAME_INFO_KEY)
        if (packageNameInfo == null) return

        val referenceToContext = JetFileReferencesResolver.resolve(file = file, visitReceivers = false)

        for ((refExpr, bindingContext) in referenceToContext) {
            if (refExpr !is JetSimpleNameExpression) continue

            val descriptor = bindingContext[BindingContext.REFERENCE_TARGET, refExpr]?.let { descriptor ->
                if (descriptor is ConstructorDescriptor) descriptor.getContainingDeclaration() else descriptor
            }
            if (descriptor == null) continue

            val packageName = DescriptorUtils.getParentOfType(
                    descriptor, javaClass<PackageFragmentDescriptor>(), false
            )?.let { DescriptorUtils.getFqName(it).toSafe() }
            when (packageName) {
                packageNameInfo.oldPackageName,
                packageNameInfo.newPackageName -> {
                    val fqName = DescriptorUtils.getFqName(descriptor)
                    if (fqName.isSafe()) {
                        (refExpr.getReference() as? JetSimpleNameReference)?.bindToFqName(fqName.toSafe(), false)
                    }
                }
            }
        }

        val packageRef = file.getPackageDirective()?.getLastReferenceExpression()?.getReference() as? JetSimpleNameReference
        packageRef?.bindToFqName(packageNameInfo.newPackageName, true)
    }
}