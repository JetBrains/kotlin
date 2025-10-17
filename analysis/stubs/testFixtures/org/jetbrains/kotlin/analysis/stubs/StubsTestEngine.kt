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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import kotlin.test.assertEquals

/**
 * Powers stub-related tests with tools to validate stubs.
 *
 * @see AbstractStubsTest
 */
abstract class StubsTestEngine {
    /**
     * Computes the stub tree for the given [KtFile]
     */
    abstract fun compute(file: KtFile): KotlinFileStubImpl

    /**
     * Validates the consistency of the given [KotlinFileStubImpl] for the given [KtFile].
     */
    open fun validate(testServices: TestServices, file: KtFile, fileStub: KotlinFileStubImpl) {
        checkPsiElementTypeConsistency(testServices.assertions, fileStub)
        validateSerializers(testServices.assertions, fileStub)
    }

    /**
     * String representation of the given [KotlinFileStubImpl].
     */
    fun render(stub: KotlinFileStubImpl): String = extractAdditionalStubInfo(stub)

    open val additionalDirectives: List<DirectivesContainer> get() = emptyList()

    private fun checkPsiElementTypeConsistency(assertions: AssertionsService, stubElement: StubElement<*>) {
        val psi = stubElement.psi as? StubBasedPsiElement<*>
        if (psi != null) {
            @Suppress("DEPRECATION") // KT-78356
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

    /**
     * Validates that the stub tree can be serialized and deserialized correctly.
     *
     * This means that a stub serializer wrote the same number of bytes to the output stream as it read during deserialization.
     */
    private fun validateSerializers(assertions: AssertionsService, fileStub: KotlinFileStubImpl) {
        val deserializedStub = serializeAndDeserializeStub(
            originalStub = fileStub,
            deserializedParentStub = null,
            buffer = UnsyncByteArrayOutputStream(),
            storage = StringEnumerator(),
        )

        assertions.assertEquals(
            expected = render(fileStub),
            actual = render(deserializedStub),
        ) { "The deserialized stub must be the same" }
    }
}


private fun <P : PsiElement, S : StubElement<P>> serializeAndDeserializeStub(
    originalStub: S,
    deserializedParentStub: StubElement<*>?,
    buffer: UnsyncByteArrayOutputStream,
    storage: AbstractStringEnumerator,
): S {
    buffer.reset()

    @Suppress("DEPRECATION") // KT-78356
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
    @Suppress("DEPRECATION") // KT-78356
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
