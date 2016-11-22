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
        val facadeSimpleName: StringRef?,
        val partSimpleName: StringRef?,
        val facadePartSimpleNames: List<StringRef?>?
) : KotlinFileStubImpl(jetFile, packageName, isScript), KotlinFileStub, PsiClassHolderFileStub<KtFile> {

    private fun StringRef.relativeToPackage() = getPackageFqName().child(Name.identifier(this.string))

    val facadeFqName: FqName?
        get() = facadeSimpleName?.relativeToPackage()

    val partFqName: FqName?
        get() = partSimpleName?.relativeToPackage()

    constructor(jetFile: KtFile?, packageName: String, isScript: Boolean)
    : this(jetFile, StringRef.fromString(packageName)!!, isScript, null, null, null)

    companion object {
        fun forFile(packageFqName: FqName, isScript: Boolean): KotlinFileStubImpl =
                KotlinFileStubForIde(jetFile = null,
                                     packageName = StringRef.fromString(packageFqName.asString())!!,
                                     facadeSimpleName = null,
                                     partSimpleName = null,
                                     facadePartSimpleNames = null,
                                     isScript = isScript)

        fun forFileFacadeStub(facadeFqName: FqName): KotlinFileStubImpl =
                KotlinFileStubForIde(jetFile = null,
                                     packageName = facadeFqName.parent().stringRef(),
                                     facadeSimpleName = facadeFqName.shortName().stringRef(),
                                     partSimpleName = facadeFqName.shortName().stringRef(),
                                     facadePartSimpleNames = null,
                                     isScript = false)

        fun forMultifileClassStub(facadeFqName: FqName, partNames: List<String>?): KotlinFileStubImpl =
                KotlinFileStubForIde(jetFile = null,
                                     packageName = facadeFqName.parent().stringRef(),
                                     facadeSimpleName = facadeFqName.shortName().stringRef(),
                                     partSimpleName = null,
                                     facadePartSimpleNames = partNames?.map { StringRef.fromString(it) },
                                     isScript = false)

        private fun FqName.stringRef() = StringRef.fromString(asString())!!
        private fun Name.stringRef() = StringRef.fromString(asString())!!
    }
}
