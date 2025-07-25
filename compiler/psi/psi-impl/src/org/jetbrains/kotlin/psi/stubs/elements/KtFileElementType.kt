/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParser
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl

object KtFileElementType : IStubFileElementType<KotlinFileStub>(KtFileElementType.NAME, KotlinLanguage.INSTANCE) {
    internal const val NAME = "kotlin.FILE"

    override fun getBuilder(): StubBuilder {
        return KtFileStubBuilder()
    }

    override fun getStubVersion(): Int {
        return KotlinStubVersions.SOURCE_STUB_VERSION
    }

    override fun getExternalId(): String {
        return NAME
    }

    override fun serialize(stub: KotlinFileStub, dataStream: StubOutputStream) {
        val fileStub = stub as KotlinFileStubImpl
        dataStream.writeName(fileStub.getPackageFqName().asString())
        dataStream.writeBoolean(fileStub.isScript())
        dataStream.writeName(fileStub.facadeFqName?.asString())
        dataStream.writeName(fileStub.partSimpleName)
        val facadePartNames = fileStub.facadePartSimpleNames
        if (facadePartNames == null) {
            dataStream.writeInt(0)
        } else {
            dataStream.writeInt(facadePartNames.size)
            for (partName in facadePartNames) {
                dataStream.writeName(partName)
            }
        }
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinFileStub {
        val packageFqNameAsString = dataStream.readNameString()
        checkNotNull(packageFqNameAsString) { "Can't read package fqname from stream" }

        val isScript = dataStream.readBoolean()
        val facadeString = dataStream.readNameString()
        val partSimpleName = dataStream.readNameString()
        val numPartNames = dataStream.readInt()
        val facadePartNames: MutableList<String> = ArrayList(numPartNames)
        for (i in 0..<numPartNames) {
            val partNameRef = dataStream.readNameString()
            checkNotNull(partNameRef) { "Can't read partName from stream" }
            facadePartNames.add(partNameRef)
        }

        return KotlinFileStubImpl(null, packageFqNameAsString, isScript, facadeString, partSimpleName, facadePartNames)
    }

    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode? {
        val project = psi.project
        val languageForParser = getLanguageForParser(psi)
        val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, chameleon.chars)
        return KotlinParser.parse(builder, psi.containingFile).firstChildNode
    }

    override fun indexStub(stub: PsiFileStub<*>, sink: IndexSink) {
        StubIndexService.getInstance().indexFile(stub as KotlinFileStub, sink)
    }
}
