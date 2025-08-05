/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.PsiFileStubImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.psi.stubs.KotlinFileStubKind
import org.jetbrains.kotlin.psi.stubs.KotlinImportAliasStub
import org.jetbrains.kotlin.psi.stubs.KotlinImportDirectiveStub
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.IMPORT_LIST
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinFileStubImpl @KtImplementationDetail internal constructor(
    ktFile: KtFile?,
    override val kind: KotlinFileStubKind,
) : PsiFileStubImpl<KtFile>(ktFile), KotlinFileStub {
    val partSimpleName: String?
        get() = (kind as? KotlinFileStubKind.WithPackage.Facade.Simple)?.partSimpleName

    val facadePartSimpleNames: List<String>?
        get() = (kind as? KotlinFileStubKind.WithPackage.Facade.MultifileClass)?.facadePartSimpleNames

    val facadeFqName: FqName?
        get() = (kind as? KotlinFileStubKind.WithPackage.Facade)?.facadeFqName

    override fun getType(): KtFileElementType = KtFileElementType

    override fun toString(): String = "${STUB_TO_STRING_PREFIX}FILE[kind=$kind]"

    override fun findImportsByAlias(alias: String): List<KotlinImportDirectiveStub> {
        val importList = childrenStubs.firstOrNull { it.stubType == IMPORT_LIST } ?: return emptyList()
        return importList.childrenStubs.filterIsInstance<KotlinImportDirectiveStub>().filter {
            it.childrenStubs.firstIsInstanceOrNull<KotlinImportAliasStub>()?.getName() == alias
        }
    }

    @OptIn(KtImplementationDetail::class)
    companion object {
        fun forFile(packageFqName: FqName): KotlinFileStubImpl = KotlinFileStubImpl(
            ktFile = null,
            kind = KotlinFileStubKindImpl.File(
                packageFqName = packageFqName
            ),
        )

        fun forScript(packageFqName: FqName): KotlinFileStubImpl = KotlinFileStubImpl(
            ktFile = null,
            kind = KotlinFileStubKindImpl.Script(
                packageFqName = packageFqName
            ),
        )

        fun forFacade(packageFqName: FqName, facadeFqName: FqName): KotlinFileStubImpl = KotlinFileStubImpl(
            ktFile = null,
            kind = KotlinFileStubKindImpl.Facade(
                packageFqName = packageFqName,
                facadeFqName = facadeFqName
            ),
        )

        fun forMultifileClass(
            packageFqName: FqName,
            facadeFqName: FqName,
            partNames: List<String>,
        ): KotlinFileStubImpl = KotlinFileStubImpl(
            ktFile = null,
            kind = KotlinFileStubKindImpl.MultifileClass(
                packageFqName = packageFqName,
                facadeFqName = facadeFqName,
                facadePartSimpleNames = partNames,
            ),
        )

        fun forInvalid(errorMessage: String): KotlinFileStubImpl = KotlinFileStubImpl(
            ktFile = null,
            kind = KotlinFileStubKindImpl.Invalid(errorMessage),
        )
    }
}
