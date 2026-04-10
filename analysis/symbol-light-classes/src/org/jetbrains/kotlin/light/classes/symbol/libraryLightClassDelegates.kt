/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypes
import org.jetbrains.kotlin.analysis.decompiled.light.classes.DecompiledLightClassesFactory
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.KotlinDeclarationInCompiledFileSearcher
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.KtClsJavaBasedLightClass
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

internal enum class CompiledFacadeKind(
    val isDeclarationFile: Boolean,
    val isMultifile: Boolean,
) {
    FILE_FACADE(isDeclarationFile = true, isMultifile = false),
    MULTIFILE_CLASS(isDeclarationFile = false, isMultifile = true),
    MULTIFILE_CLASS_PART(isDeclarationFile = true, isMultifile = true),
}

internal data class CompiledFacadeFileInfo(
    val facadeFqName: FqName,
    val kind: CompiledFacadeKind,
)

internal fun KtFile.compiledFacadeFileInfo(): CompiledFacadeFileInfo? {
    val file = this as? KtClsFile ?: return null
    val binaryClass = KotlinBinaryClassCache
        .getKotlinBinaryClassOrClassFileContent(file.virtualFile, MetadataVersion.INSTANCE)
        ?.let { it as? KotlinClassFinder.Result.KotlinClass }
        ?.kotlinJvmBinaryClass
        ?: return null
    val header = binaryClass.classHeader

    return when (header.kind) {
        KotlinClassHeader.Kind.FILE_FACADE ->
            CompiledFacadeFileInfo(binaryClass.classId.asSingleFqName(), CompiledFacadeKind.FILE_FACADE)

        KotlinClassHeader.Kind.MULTIFILE_CLASS ->
            CompiledFacadeFileInfo(binaryClass.classId.asSingleFqName(), CompiledFacadeKind.MULTIFILE_CLASS)

        KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
            val multifileClassName = header.multifileClassName ?: return null
            CompiledFacadeFileInfo(
                JvmClassName.byInternalName(multifileClassName).fqNameForTopLevelClassMaybeWithDollars,
                CompiledFacadeKind.MULTIFILE_CLASS_PART,
            )
        }

        else -> null
    }
}

internal data class BinaryLightClassDelegate(
    val file: KtClsFile,
    val clsDelegate: PsiClass,
    private val useSignatureFallback: Boolean = false,
) {
    private val searcher: KotlinDeclarationInCompiledFileSearcher
        get() = KotlinDeclarationInCompiledFileSearcher.getInstance()

    fun findMethod(
        targetDeclaration: KtDeclaration?,
        parameterCount: Int,
        preferredName: String? = null,
        isGetter: Boolean? = null,
    ): PsiMethod? {
        val signatureCandidates = clsDelegate.methods.filter { method ->
            method.parameterList.parametersCount == parameterCount &&
                    (isGetter == null || (method.returnType != PsiTypes.voidType()) == isGetter)
        }

        val declarationCandidates = targetDeclaration?.let { declaration ->
            signatureCandidates.filter { method ->
                matches(declaration, searcher.findDeclarationInCompiledFile(file, method))
            }
        }.orEmpty()

        return selectCandidate(declarationCandidates, preferredName)
            ?: if (useSignatureFallback) selectFallbackCandidate(signatureCandidates, preferredName) else null
    }

    private fun selectCandidate(
        candidates: List<PsiMethod>,
        preferredName: String?,
    ): PsiMethod? {
        return if (preferredName != null) {
            candidates.firstOrNull { it.name == preferredName } ?: candidates.firstOrNull()
        } else {
            candidates.firstOrNull()
        }
    }

    private fun selectFallbackCandidate(
        candidates: List<PsiMethod>,
        preferredName: String?,
    ): PsiMethod? {
        return if (preferredName != null) {
            candidates.firstOrNull { it.name == preferredName }
        } else {
            candidates.firstOrNull()
        }
    }

    fun findField(
        targetDeclaration: KtDeclaration?,
        preferredName: String? = null,
    ): PsiField? {
        val declarationCandidates = targetDeclaration?.let { declaration ->
            clsDelegate.fields.filter { field ->
                matches(declaration, searcher.findDeclarationInCompiledFile(file, field))
            }
        }.orEmpty()

        return selectCandidate(declarationCandidates, preferredName)
            ?: if (useSignatureFallback) selectFallbackCandidate(clsDelegate.fields.toList(), preferredName) else null
    }

    private fun selectCandidate(
        candidates: List<PsiField>,
        preferredName: String?,
    ): PsiField? {
        return if (preferredName != null) {
            candidates.firstOrNull { it.name == preferredName } ?: candidates.firstOrNull()
        } else {
            candidates.firstOrNull()
        }
    }

    private fun selectFallbackCandidate(
        candidates: List<PsiField>,
        preferredName: String?,
    ): PsiField? {
        return if (preferredName != null) {
            candidates.firstOrNull { it.name == preferredName }
        } else {
            candidates.firstOrNull()
        }
    }

    private fun matches(targetDeclaration: KtDeclaration, candidate: KtDeclaration?): Boolean {
        return candidate != null && (candidate == targetDeclaration || candidate.isEquivalentTo(targetDeclaration))
    }
}

internal fun createBinaryLightClassDelegate(
    project: Project,
    classOrObjectDeclaration: KtClassOrObject?,
): BinaryLightClassDelegate? {
    val decompiledDeclaration = classOrObjectDeclaration ?: return null
    val file = decompiledDeclaration.containingKtFile as? KtClsFile ?: return null
    val lightClass = DecompiledLightClassesFactory.getLightClassForDecompiledClassOrObject(decompiledDeclaration, project)
        as? KtClsJavaBasedLightClass ?: return null
    return BinaryLightClassDelegate(file, lightClass.clsDelegate)
}

internal fun createBinaryLightClassDelegate(
    project: Project,
    facadeClassFqName: FqName,
    files: Collection<KtFile>,
): BinaryLightClassDelegate? {
    val file = files.firstOrNull { it.javaFileFacadeFqName == facadeClassFqName } as? KtClsFile ?: return null
    val lightClass = DecompiledLightClassesFactory.createLightFacadeForDecompiledKotlinFile(project, facadeClassFqName, files.toList())
        as? KtClsJavaBasedLightClass ?: return null
    return BinaryLightClassDelegate(file, lightClass.clsDelegate, useSignatureFallback = true)
}
