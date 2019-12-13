/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.psi.stubs.PsiClassHolderFileStub
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl

class KotlinFileStubForIde(
    jetFile: KtFile?,
    packageName: StringRef,
    isScript: Boolean,
    private val facadeFqNameRef: StringRef?,
    val partSimpleName: StringRef?,
    val facadePartSimpleNames: List<StringRef?>?
) : KotlinFileStubImpl(jetFile, packageName, isScript), KotlinFileStub, PsiClassHolderFileStub<KtFile> {

    private fun StringRef.relativeToPackage() = getPackageFqName().child(Name.identifier(this.string))

    val partFqName: FqName?
        get() = partSimpleName?.relativeToPackage()
    val facadeFqName: FqName?
        get() = facadeFqNameRef?.let { FqName(it.string) }

    constructor(jetFile: KtFile?, packageName: String, isScript: Boolean) : this(
        jetFile,
        StringRef.fromString(packageName)!!,
        isScript,
        null,
        null,
        null
    )

    companion object {
        fun forFile(packageFqName: FqName, isScript: Boolean): KotlinFileStubImpl = KotlinFileStubForIde(
            jetFile = null,
            packageName = StringRef.fromString(packageFqName.asString())!!,
            facadeFqNameRef = null,
            partSimpleName = null,
            facadePartSimpleNames = null,
            isScript = isScript
        )

        fun forFileFacadeStub(facadeFqName: FqName): KotlinFileStubImpl = KotlinFileStubForIde(
            jetFile = null,
            packageName = facadeFqName.parent().stringRef(),
            facadeFqNameRef = facadeFqName.stringRef(),
            partSimpleName = facadeFqName.shortName().stringRef(),
            facadePartSimpleNames = null,
            isScript = false
        )

        fun forMultifileClassStub(facadeFqName: FqName, partNames: List<String>?): KotlinFileStubImpl = KotlinFileStubForIde(
            jetFile = null,
            packageName = facadeFqName.parent().stringRef(),
            facadeFqNameRef = facadeFqName.stringRef(),
            partSimpleName = null,
            facadePartSimpleNames = partNames?.map { StringRef.fromString(it) },
            isScript = false
        )

        private fun FqName.stringRef() = StringRef.fromString(asString())!!
        private fun Name.stringRef() = StringRef.fromString(asString())!!
    }
}
