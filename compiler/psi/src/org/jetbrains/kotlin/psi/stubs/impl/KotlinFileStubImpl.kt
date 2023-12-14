/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

    constructor(ktFile: KtFile?, packageName: String, isScript: Boolean) : this(
        ktFile,
        packageName,
        isScript,
        facadeFqNameString = null,
        partSimpleName = null,
        facadePartSimpleNames = null
    )

    private fun String.relativeToPackage() = getPackageFqName().child(Name.identifier(this))

    val partFqName: FqName?
        get() = partSimpleName?.relativeToPackage()

    val facadeFqName: FqName?
        get() = facadeFqNameString?.let(::FqName)

    override fun getPackageFqName(): FqName = FqName(packageName)
    override fun isScript(): Boolean = isScript
    override fun getType(): IStubFileElementType<KotlinFileStub> = KtFileElementType.INSTANCE

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
