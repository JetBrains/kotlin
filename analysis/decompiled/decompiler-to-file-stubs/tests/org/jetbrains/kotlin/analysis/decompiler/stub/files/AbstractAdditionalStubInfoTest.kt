/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.files

import com.intellij.psi.stubs.StubElement
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinClsStubBuilder
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.nio.file.Paths

abstract class AbstractAdditionalStubInfoTest : AbstractDecompiledClassTest() {
    fun runTest(testDirectory: String) {
        val testDirectoryPath = Paths.get(testDirectory)
        val testData = TestData.createFromDirectory(testDirectoryPath)
        val stub = KotlinClsStubBuilder().buildFileStub(FileContentImpl.createByFile(getClassFileToDecompile(testData, false)))!!
        val builder = StringBuilder()
        extractAdditionInfo(stub, builder, 0)
        KotlinTestUtils.assertEqualsToFile(testData.expectedFile, builder.toString())
    }

    private fun extractAdditionInfo(stub: StubElement<*>, builder: StringBuilder, level: Int) {
        builder.append(stub.toString())
        if (stub is KotlinUserTypeStubImpl) {
            val upperBound = stub.upperBound
            if (upperBound != null) {
                builder.append("    ft: ")
                appendFlexibleTypeInfo(builder, upperBound)
            }
        }
        for (child in stub.childrenStubs) {
            builder.append("\n").append("  ".repeat(level))
            extractAdditionInfo(child, builder, level + 1)
        }
    }

    private fun appendFlexibleTypeInfo(builder: StringBuilder, typeBean: KotlinTypeBean) {
        when (typeBean) {
            is KotlinClassTypeBean -> {
                builder.append(typeBean.classId.asFqNameString())
                val arguments = typeBean.arguments
                if (arguments.isNotEmpty()) {
                    builder.append("<")
                    arguments.forEachIndexed { index, arg ->
                        if (index > 0) builder.append(", ")
                        if (arg.projectionKind != KtProjectionKind.NONE) {
                            builder.append(arg.projectionKind.name)
                        }
                        if (arg.projectionKind != KtProjectionKind.STAR) {
                            appendFlexibleTypeInfo(builder, arg.type!!)
                        }
                    }
                    builder.append(">")
                }
                if (typeBean.nullable) {
                    builder.append("?")
                }
            }
            is KotlinTypeParameterTypeBean -> {
                builder.append(typeBean.typeParameterName)
                if (typeBean.nullable) {
                    builder.append("?")
                }
                if (typeBean.definitelyNotNull) {
                    builder.append(" & Any")
                }
            }

            is KotlinFlexibleTypeBean -> {
                appendFlexibleTypeInfo(builder, typeBean.lowerBound)
                builder.append(" .. ")
                appendFlexibleTypeInfo(builder, typeBean.upperBound)
            }
        }
    }
}