package org.jetbrains.konan.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor

class AdditionalKotlinReferencesInSwiftAndObjCRenamer : AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement): Boolean {
        return element is KtClassOrObject || element is KtParameter
    }

    override fun createRenamer(element: PsiElement?, newName: String?, usages: MutableCollection<UsageInfo>?): AutomaticRenamer {
        return object : AutomaticRenamer() {
            init {
                when (element) {
                    is KtClassOrObject -> {
                        element.acceptChildren(classOrObjectRecursiveVisitor {
                            myElements.add(it)
                        })
                    }
                    is KtParameter -> {
                        (element.ownerFunction as? KtNamedFunction)?.let {
                            myElements.add(it)
                        }
                    }
                }
            }

            override fun getNewName(namedElement: PsiNamedElement?): String? = namedElement?.name

            override fun hasAnythingToRename(): Boolean = false
            override fun getDialogDescription(): String = ""
            override fun getDialogTitle(): String = ""
            override fun entityName(): String = ""
        }
    }

    override fun isEnabled(): Boolean = true

    override fun setEnabled(enabled: Boolean) {
    }

    override fun getOptionName(): String? = null
}