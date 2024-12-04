/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProvider
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.builder.ClsWrapperStubPsiFactory
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

object DecompiledLightClassesFactory {
    private val checkInconsistency: Boolean
        get() = Registry.`is`(
            /* key = */ "kotlin.decompiled.light.classes.check.inconsistency",
            /* defaultValue = */ false,
        )

    fun getLightClassForDecompiledClassOrObject(
        decompiledClassOrObject: KtClassOrObject,
        project: Project
    ): KtLightClassForDecompiledDeclaration? {
        if (decompiledClassOrObject is KtEnumEntry) {
            return null
        }
        val containingKtFile = decompiledClassOrObject.containingFile as? KtClsFile ?: return null
        val rootLightClassForDecompiledFile = createLightClassForDecompiledKotlinFile(containingKtFile, project) ?: return null

        return findCorrespondingLightClass(decompiledClassOrObject, rootLightClassForDecompiledFile)
    }

    private fun findCorrespondingLightClass(
        decompiledClassOrObject: KtClassOrObject,
        rootLightClassForDecompiledFile: KtLightClassForDecompiledDeclaration
    ): KtLightClassForDecompiledDeclaration? {
        val relativeFqName = getClassRelativeName(decompiledClassOrObject) ?: return null
        val iterator = relativeFqName.pathSegments().iterator()
        val base = iterator.next()

        // In case class files have been obfuscated (i.e., SomeClass belongs to a.class file), just ignore them
        if (rootLightClassForDecompiledFile.name != base.asString()) return null

        var current: KtLightClassForDecompiledDeclaration = rootLightClassForDecompiledFile
        while (iterator.hasNext()) {
            val name = iterator.next()
            val innerClass = current.findInnerClassByName(name.asString(), false)
            current = when {
                innerClass != null -> innerClass as KtLightClassForDecompiledDeclaration
                checkInconsistency -> {
                    throw KotlinExceptionWithAttachments("Could not find corresponding inner/nested class")
                        .withAttachment("relativeFqName.txt", relativeFqName)
                        .withAttachment("decompiledClassOrObjectFqName.txt", decompiledClassOrObject.fqName)
                        .withAttachment("decompiledFileName.txt", decompiledClassOrObject.containingKtFile.virtualFile.name)
                        .withPsiAttachment("decompiledClassOrObject.txt", decompiledClassOrObject)
                        .withAttachment("fileClass.txt", decompiledClassOrObject.containingFile::class)
                        .withPsiAttachment("file.txt", decompiledClassOrObject.containingFile)
                        .withPsiAttachment("root.txt", rootLightClassForDecompiledFile)
                        .withAttachment("currentName.txt", current.name)
                        .withPsiAttachment("current.txt", current)
                        .withAttachment("innerClasses.txt", current.innerClasses.map { psiClass -> psiClass.name })
                        .withAttachment("innerName.txt", name.asString())
                }

                else -> return null
            }
        }

        return current
    }

    private fun getClassRelativeName(decompiledClassOrObject: KtClassOrObject): FqName? {
        val name = decompiledClassOrObject.nameAsName ?: return null
        val parent = PsiTreeUtil.getParentOfType(
            decompiledClassOrObject,
            KtClassOrObject::class.java,
            true
        )
        if (parent == null) {
            assert(decompiledClassOrObject.isTopLevel())
            return FqName.topLevel(name)
        }
        return getClassRelativeName(parent)?.child(name)
    }

    fun createLightClassForDecompiledKotlinFile(file: KtClsFile, project: Project): KtLightClassForDecompiledDeclaration? {
        return createLightClassForDecompiledKotlinFile(project, file) { kotlinClsFile, javaClsClass, classOrObject ->
            KtLightClassForDecompiledDeclaration(javaClsClass, javaClsClass.parent, kotlinClsFile, classOrObject)
        }
    }

    private fun <T> createLightClassForDecompiledKotlinFile(
        project: Project,
        file: KtClsFile,
        builder: (kotlinClsFile: KtClsFile, javaClsClass: PsiClass, classOrObject: KtClassOrObject?) -> T
    ): T? {
        val virtualFile = file.virtualFile ?: return null
        val classOrObject = file.declarations.filterIsInstance<KtClassOrObject>().singleOrNull()
        val javaClsClass = createClsJavaClassFromVirtualFile(
            mirrorFile = file,
            classFile = virtualFile,
            correspondingClassOrObject = classOrObject,
            project = project,
        ) ?: return null

        return builder(file, javaClsClass, classOrObject)
    }

    fun createLightFacadeForDecompiledKotlinFile(
        project: Project,
        facadeClassFqName: FqName,
        files: List<KtFile>,
    ): KtLightClassForFacade? {
        assert(files.all(KtFile::isCompiled))
        val file = files.firstOrNull { it.javaFileFacadeFqName == facadeClassFqName } as? KtClsFile
            ?: error("Can't find the representative decompiled file for $facadeClassFqName in ${files.map { it.name }}")

        return createLightClassForDecompiledKotlinFile(project, file) { kotlinClsFile, javaClsClass, classOrObject ->
            KtLightClassForDecompiledFacade(javaClsClass, javaClsClass.parent, kotlinClsFile, classOrObject, files)
        }
    }

    fun createClsJavaClassFromVirtualFile(
        mirrorFile: KtFile,
        classFile: VirtualFile,
        correspondingClassOrObject: KtClassOrObject?,
        project: Project,
    ): ClsClassImpl? {
        val javaFileStub = ClsJavaStubByVirtualFileCache.getInstance(project).get(classFile) ?: return null
        javaFileStub.psiFactory = ClsWrapperStubPsiFactory.INSTANCE
        val manager = PsiManager.getInstance(mirrorFile.project)
        val fakeFile = object : ClsFileImpl(ClassFileViewProvider(manager, classFile)) {
            override fun getNavigationElement(): PsiElement {
                if (correspondingClassOrObject != null) {
                    return correspondingClassOrObject.navigationElement.containingFile
                }
                return super.getNavigationElement()
            }

            override fun getStub() = javaFileStub

            override fun getMirror() = mirrorFile

            override fun isPhysical() = false
        }
        javaFileStub.psi = fakeFile
        return fakeFile.classes.single() as ClsClassImpl
    }
}