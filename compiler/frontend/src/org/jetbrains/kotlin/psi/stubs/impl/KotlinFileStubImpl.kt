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

import com.intellij.psi.PsiClass
import com.intellij.psi.impl.java.stubs.PsiClassStub
import com.intellij.psi.stubs.PsiClassHolderFileStub
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.psi.stubs.KotlinImportDirectiveStub
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes

// SCRIPT: PsiJetFileStubImpl knows about scripting
public class KotlinFileStubImpl(
        jetFile: JetFile?,
        private val packageName: StringRef,
        private val facadeSimpleName: StringRef?,
        private val partSimpleName: StringRef?,
        private val isScript: Boolean
) : PsiFileStubImpl<JetFile>(jetFile), KotlinFileStub, PsiClassHolderFileStub<JetFile> {

    public constructor(jetFile: JetFile?, packageName: String, isScript: Boolean)
        : this(jetFile, StringRef.fromString(packageName)!!, null, null, isScript)

    override fun getPackageFqName(): FqName = FqName(StringRef.toString(packageName)!!)
    override fun getFacadeSimpleName(): String? = StringRef.toString(facadeSimpleName)
    override fun getPartSimpleName(): String? = StringRef.toString(partSimpleName)
    override fun isScript(): Boolean = isScript
    override fun getType(): IStubFileElementType<KotlinFileStub> = JetStubElementTypes.FILE

    override fun toString(): String = "PsiJetFileStubImpl[" + "package=" + getPackageFqName().asString() + "]"

    override fun getClasses(): Array<PsiClass> {
        return childrenStubs.filterIsInstance<PsiClassStub<*>>().map { it.psi }.toTypedArray()
    }

    override fun findImportsByAlias(alias: String): List<KotlinImportDirectiveStub> {
        val importList = childrenStubs.firstOrNull { it.stubType == JetStubElementTypes.IMPORT_LIST } ?: return emptyList()
        return importList.childrenStubs.filterIsInstance<KotlinImportDirectiveStub>().filter { it.getAliasName() == alias }
    }

    companion object {
        public fun forPackageStub(packageFqName: FqName, isScript: Boolean): KotlinFileStubImpl =
                KotlinFileStubImpl(jetFile = null,
                                   packageName = StringRef.fromString(packageFqName.asString())!!,
                                   facadeSimpleName = null,
                                   partSimpleName = null,
                                   isScript = isScript)

        public fun forFileFacadeStub(facadeFqName: FqName, isScript: Boolean): KotlinFileStubImpl =
                KotlinFileStubImpl(jetFile = null,
                                   packageName = facadeFqName.parent().stringRef(),
                                   facadeSimpleName = facadeFqName.shortName().stringRef(),
                                   partSimpleName = facadeFqName.shortName().stringRef(),
                                   isScript = isScript)

        public fun forMultifileClassStub(facadeFqName: FqName, isScript: Boolean): KotlinFileStubImpl =
                KotlinFileStubImpl(jetFile = null,
                                   packageName = facadeFqName.parent().stringRef(),
                                   facadeSimpleName = facadeFqName.shortName().stringRef(),
                                   partSimpleName = null,
                                   isScript = isScript)

        private fun FqName.stringRef() = StringRef.fromString(asString())!!
        private fun Name.stringRef() = StringRef.fromString(asString())!!
    }
}
