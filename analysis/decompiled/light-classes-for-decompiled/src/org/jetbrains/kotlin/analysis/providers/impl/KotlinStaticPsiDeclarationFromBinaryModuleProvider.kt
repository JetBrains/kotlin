/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.decompiled.light.classes.ClsJavaStubByVirtualFileCache
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.providers.KotlinPsiDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPsiDeclarationProviderFactory
import org.jetbrains.kotlin.asJava.builder.ClsWrapperStubPsiFactory
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart

private class KotlinStaticPsiDeclarationFromBinaryModuleProvider(
    private val project: Project,
    override val scope: GlobalSearchScope,
    private val binaryModules: Collection<KtBinaryModule>,
    override val jarFileSystem: CoreJarFileSystem,
) : KotlinPsiDeclarationProvider(), AbstractDeclarationFromBinaryModuleProvider {
    private val psiManager by lazy { PsiManager.getInstance(project) }

    private fun clsClassImplsByFqName(
        fqName: FqName,
        isPackageName: Boolean = true,
    ): Collection<ClsClassImpl> {
        return binaryModules
            .flatMap {
                virtualFilesFromModule(it, fqName, isPackageName)
            }
            .mapNotNull {
                createClsJavaClassFromVirtualFile(it)
            }
    }

    private fun createClsJavaClassFromVirtualFile(
        classFile: VirtualFile,
    ): ClsClassImpl? {
        val javaFileStub = ClsJavaStubByVirtualFileCache.getInstance(project).get(classFile) ?: return null
        javaFileStub.psiFactory = ClsWrapperStubPsiFactory.INSTANCE
        val fakeFile = object : ClsFileImpl(ClassFileViewProvider(psiManager, classFile)) {
            override fun getStub() = javaFileStub

            override fun isPhysical() = false
        }
        javaFileStub.psi = fakeFile
        return fakeFile.classes.single() as ClsClassImpl
    }

    override fun getClassesByClassId(classId: ClassId): Collection<ClsClassImpl> {
        return clsClassImplsByFqName(classId.asSingleFqName(), isPackageName = false)
    }

    override fun getProperties(callableId: CallableId): Collection<PsiMember> {
        val classes = callableId.classId?.let { classId ->
            getClassesByClassId(classId)
        } ?: clsClassImplsByFqName(callableId.packageName)
        return classes.flatMap { psiClass ->
            psiClass.children
                .filterIsInstance<PsiMember>()
                .filter { psiMember ->
                    if (psiMember !is PsiMethod && psiMember !is PsiField) return@filter false
                    val name = psiMember.name ?: return@filter false
                    // PsiField a.k.a. backing field
                    name == callableId.callableName.identifier ||
                            // PsiMethod, i.e., accessors
                            (name.startsWith("get") || name.startsWith("set")) &&
                            // E.g., getFooBar -> FooBar -> fooBar
                            (name.substring(3).decapitalizeSmart().endsWith(callableId.callableName.identifier))

                }
        }.toList()
    }

    override fun getFunctions(callableId: CallableId): Collection<PsiMethod> {
        val classes = callableId.classId?.let { classId ->
            getClassesByClassId(classId)
        } ?: clsClassImplsByFqName(callableId.packageName)
        return classes.flatMap { psiClass ->
            psiClass.methods.filter { psiMethod ->
                psiMethod.name == callableId.callableName.identifier
            }
        }.toList()
    }
}

// TODO: we can't register this in IDE yet due to non-trivial parameters: lib modules and jar file system.
//  We need a session or facade that maintains such information
public class KotlinStaticPsiDeclarationProviderFactory(
    private val project: Project,
    private val binaryModules: Collection<KtBinaryModule>,
    private val jarFileSystem: CoreJarFileSystem,
) : KotlinPsiDeclarationProviderFactory() {
    override fun createPsiDeclarationProvider(searchScope: GlobalSearchScope): KotlinPsiDeclarationProvider {
        return KotlinStaticPsiDeclarationFromBinaryModuleProvider(project, searchScope, binaryModules, jarFileSystem)
    }
}
