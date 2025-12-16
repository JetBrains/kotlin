/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.ObjectStubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment

object Utils {
    fun wrapStrings(names: List<String>): Array<StringRef> = if (names.isEmpty())
        StringRef.EMPTY_ARRAY
    else
        Array(names.size) { i -> StringRef.fromString(names[i])!! }
}


/** Creates a deep copy of the given [this] */
@KtImplementationDetail
fun KotlinFileStubImpl.deepCopy(): KotlinFileStubImpl = copyStubRecursively(
    originalStub = this,
    newParentStub = null,
) as KotlinFileStubImpl

/**
 * Returns a copy of [originalStub].
 */
@OptIn(KtImplementationDetail::class)
private fun <T : PsiElement> copyStubRecursively(
    originalStub: StubElement<T>,
    newParentStub: StubElement<*>?,
): StubElement<*> {
    require(originalStub is KotlinStubElement<*>) {
        "${KotlinStubElement::class.simpleName} is expected, but ${originalStub::class.simpleName} is found"
    }

    val stubCopy = originalStub.copyInto(newParentStub)
    if (originalStub is ObjectStubBase<*> && originalStub.isDangling) {
        (stubCopy as ObjectStubBase<*>).markDangling()
    }

    checkWithAttachment(
        originalStub::class == stubCopy::class,
        { "${originalStub::class.simpleName} is expected, but ${stubCopy::class.simpleName} is found" },
    )

    for (originalChild in originalStub.childrenStubs) {
        copyStubRecursively(originalStub = originalChild, newParentStub = stubCopy)
    }

    return stubCopy
}
