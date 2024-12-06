package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiPackage
import com.intellij.psi.impl.file.impl.JavaFileManager
import com.intellij.psi.search.GlobalSearchScope

class JavaFileManagerImpl : JavaFileManager {
    override fun findPackage(packageName: String): PsiPackage? {
        return null
    }

    override fun findClass(qName: String, scope: GlobalSearchScope): PsiClass? {
        return null
    }

    override fun findClasses(qName: String, scope: GlobalSearchScope): Array<PsiClass> {
        return emptyArray()
    }

    override fun getNonTrivialPackagePrefixes(): MutableCollection<String> {
        return arrayListOf()
    }

    override fun findModules(moduleName: String, scope: GlobalSearchScope): MutableCollection<PsiJavaModule> {
        return arrayListOf()
    }

}
