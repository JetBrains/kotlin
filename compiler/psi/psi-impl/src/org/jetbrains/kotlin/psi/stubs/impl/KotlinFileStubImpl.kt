/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.psi.stubs.KotlinImportAliasStub
import org.jetbrains.kotlin.psi.stubs.KotlinImportDirectiveStub
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.IMPORT_LIST
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinFileStubImpl(
    ktFile: KtFile?,
    private val packageName: String,
    private val isScript: Boolean,
    private val facadeFqNameString: String?,
    val partSimpleName: String?,
    val facadePartSimpleNames: List<String>?,
) : PsiFileStubImpl<KtFile>(ktFile), KotlinFileStub {
    private fun String.relativeToPackage() = getPackageFqName().child(Name.identifier(this))

    /**
     * Is used from [org.jetbrains.kotlin.psi.stubs.elements.StubIndexService] implementation on the plugin side
     */
    @Suppress("unused")
    val partFqName: FqName?
        get() = partSimpleName?.relativeToPackage()

    val facadeFqName: FqName?
        get() = facadeFqNameString?.let(::FqName)

    override fun getPackageFqName(): FqName = FqName(packageName)
    override fun isScript(): Boolean = isScript
    override fun getType(): IStubFileElementType<KotlinFileStub> = KtFileElementType

    override fun toString(): String = "PsiJetFileStubImpl[" + "package=" + getPackageFqName().asString() + "]"

    override fun findImportsByAlias(alias: String): List<KotlinImportDirectiveStub> {
        val importList = childrenStubs.firstOrNull { it.stubType == IMPORT_LIST } ?: return emptyList()
        return importList.childrenStubs.filterIsInstance<KotlinImportDirectiveStub>().filter {
            it.childrenStubs.firstIsInstanceOrNull<KotlinImportAliasStub>()?.getName() == alias
        }
    }

    companion object {
        fun forFile(packageFqName: FqName, isScript: Boolean): KotlinFileStubImpl = KotlinFileStubImpl(
            ktFile = null,
            packageName = packageFqName.asString(),
            facadeFqNameString = null,
            partSimpleName = null,
            facadePartSimpleNames = null,
            isScript = isScript
        )

        fun forFileFacadeStub(facadeFqName: FqName): KotlinFileStubImpl = KotlinFileStubImpl(
            ktFile = null,
            packageName = facadeFqName.parent().asString(),
            facadeFqNameString = facadeFqName.asString(),
            partSimpleName = facadeFqName.shortName().asString(),
            facadePartSimpleNames = null,
            isScript = false
        )

        fun forMultifileClassStub(packageFqName: FqName, facadeFqName: FqName, partNames: List<String>?): KotlinFileStubImpl =
            KotlinFileStubImpl(
                ktFile = null,
                packageName = packageFqName.asString(),
                facadeFqNameString = facadeFqName.asString(),
                partSimpleName = null,
                facadePartSimpleNames = partNames,
                isScript = false,
            )
    }
}
