/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.PsiElement
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.AbstractStringEnumerator
import com.intellij.util.io.UnsyncByteArrayOutputStream
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.analysis.decompiler.stub.files.serializeToString
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtLibraryBinaryDecompiledTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import kotlin.test.assertEquals

abstract class AbstractStubsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        val files = mainModule.ktFiles
        val filesAndStubs = files.sortedBy(KtFile::getName).map { it to computeStub(it) }

        val actual = prettyPrint {
            if (filesAndStubs.isEmpty()) {
                appendLine("NO FILES")
                return@prettyPrint
            }

            val singleElement = filesAndStubs.singleOrNull()
            if (singleElement != null) {
                printStub(singleElement.second)
            } else {
                printCollection(filesAndStubs, separator = "\n\n") { element ->
                    appendLine("${element.first.name}:")
                    withIndent {
                        printStub(element.second)
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = ".stubs.txt")

        for ((_, stub) in filesAndStubs) {
            val fileStub = stub ?: continue
            checkPsiElementTypeConsistency(testServices.assertions, fileStub)
            validateSerializers(testServices.assertions, fileStub)
        }
    }

    /**
     * Validates that the stub tree can be serialized and deserialized correctly.
     *
     * This means that a stub serializer wrote the same number of bytes to the output stream as it read during deserialization.
     */
    private fun validateSerializers(assertions: AssertionsService, fileStub: PsiFileStub<*>) {
        val deserializedStub = serializeAndDeserializeStub(
            originalStub = fileStub,
            deserializedParentStub = null,
            buffer = UnsyncByteArrayOutputStream(),
            storage = StringEnumerator(),
        )

        assertions.assertEquals(
            expected = renderStub(fileStub),
            actual = renderStub(deserializedStub),
        ) { "The deserialized stub must be the same" }
    }

    private fun checkPsiElementTypeConsistency(assertions: AssertionsService, stubElement: StubElement<*>) {
        val psi = stubElement.psi as? StubBasedPsiElement<*>
        if (psi != null) {
            assertions.assertEquals(
                stubElement.stubType,
                psi.elementType,
            ) {
                "Expected the PSI of `$stubElement` to have the same element type. Instead got: `${psi.elementType}`."
            }
        }

        stubElement.childrenStubs.forEach {
            checkPsiElementTypeConsistency(assertions, it)
        }
    }

    context(printer: PrettyPrinter)
    private fun printStub(stub: PsiFileStub<*>?) {
        val stubRepresentation = renderStub(stub)
        printer.append(stubRepresentation)
    }

    protected open fun renderStub(stub: PsiFileStub<*>?): String = stub?.serializeToString().toString()

    abstract fun computeStub(file: KtFile): PsiFileStub<*>?
}

abstract class AbstractSourceStubsTest : AbstractStubsTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun computeStub(file: KtFile): PsiFileStub<*> = file.calcStubTree().root
}

abstract class AbstractCompiledStubsTest : AbstractStubsTest() {
    override val configurator: AnalysisApiTestConfigurator = CompiledStubsTestConfigurator()

    override fun computeStub(file: KtFile): PsiFileStub<*>? = ClsClassFinder.allowMultifileClassPart {
        StubTreeLoader.getInstance()
            .build(/* project = */ null, /* vFile = */ file.virtualFile, /* psiFile = */ null)
            ?.root
            ?.let { it as PsiFileStub<*> }
    }
}

private open class CompiledStubsTestConfigurator : AnalysisApiFirBinaryTestConfigurator() {
    override val testModuleFactory: KtTestModuleFactory get() = KtLibraryBinaryDecompiledTestModuleFactory
    override val testPrefixes: List<String> get() = listOf("compiled")
}

abstract class AbstractDecompiledStubsTest : AbstractStubsTest() {
    override val configurator: AnalysisApiTestConfigurator = object : CompiledStubsTestConfigurator() {
        override val testPrefixes: List<String> get() = listOf("decompiled") + super.testPrefixes
    }

    override fun computeStub(file: KtFile): PsiFileStub<*> = file.calcStubTree().root
}

private fun <P : PsiElement, S : StubElement<P>> serializeAndDeserializeStub(
    originalStub: S,
    deserializedParentStub: StubElement<*>?,
    buffer: UnsyncByteArrayOutputStream,
    storage: AbstractStringEnumerator,
): S {
    buffer.reset()

    val serializer = if (originalStub is PsiFileStub<*>) {
        originalStub.type
    } else {
        originalStub.stubType
    }

    @Suppress("UNCHECKED_CAST")
    serializer as ObjectStubSerializer<StubElement<*>, StubElement<*>>

    serializer.serialize(originalStub, StubOutputStream(buffer, storage))

    val stubInputStream = StubInputStream(buffer.toInputStream(), storage)
    val deserializedStub = serializer.deserialize(stubInputStream, deserializedParentStub)
    assertEquals(-1, stubInputStream.read(), "The deserializer has to read the same amount of bytes as the serializer wrote")
    assertEquals(originalStub::class, deserializedStub::class, "The stub class must be the same")
    assertEquals(originalStub.stubType, deserializedStub.stubType, "The stub type must be the same")

    for (originalChild in originalStub.childrenStubs) {
        serializeAndDeserializeStub(
            originalStub = originalChild,
            deserializedParentStub = deserializedStub,
            buffer = buffer,
            storage = storage,
        )
    }

    @Suppress("UNCHECKED_CAST")
    return deserializedStub as S
}

private class StringEnumerator : AbstractStringEnumerator {
    private val values = HashMap<String, Int>()
    private val strings = mutableListOf<String>()

    override fun enumerate(value: String?): Int {
        if (value == null) return 0

        return values.getOrPut(value) {
            strings += value
            values.size + 1
        }
    }

    override fun valueOf(idx: Int): String? = if (idx == 0) null else strings[idx - 1]

    override fun markCorrupted(): Unit = shouldNotBeCalled()
    override fun close(): Unit = shouldNotBeCalled()
    override fun isDirty(): Boolean = shouldNotBeCalled()
    override fun force(): Unit = shouldNotBeCalled()
}
