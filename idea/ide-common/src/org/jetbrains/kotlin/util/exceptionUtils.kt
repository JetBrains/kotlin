/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import java.lang.IllegalStateException
import kotlin.reflect.KClass

class ExceptionWithAttachmentWrapper(val ref: String, val varsProvider: () -> Array<Any?>?) {
    var catch: KClass<out Throwable> = Throwable::class

    fun <T> invoke(block: () -> T) =
        wrapExceptionWithAttachments(block)

    inline fun <reified T : Throwable> catch(): ExceptionWithAttachmentWrapper {
        catch = T::class
        return this
    }

    private fun <T> wrapExceptionWithAttachments(
        block: () -> T
    ): T {
        try {
            return block.invoke()
        } catch (t: Throwable) {
            if (catch.java.isAssignableFrom(t.javaClass)) {
                val exception = KotlinExceptionWithAttachments(ref + t.message, t)
                val vars = varsProvider.invoke() ?: emptyArray()
                for (arg in vars.withIndex()) {
                    arg.value?.let {
                        exception.withAttachment("arg" + arg.index, arg.value.toString())
                    }
                }
                throw exception
            }
            throw t;
        }
    }
}

object EA {
    inline fun <T> vars(vararg elements: T): () -> Array<Any?>? =
        { arrayOf(elements) }

    fun ea141456(f: (EA) -> () -> Array<Any?>?) =
        ExceptionWithAttachmentWrapper("EA-141456", f(EA)).catch<IllegalStateException>()

    fun ea219323(f: (EA) -> () -> Array<Any?>?) =
        ExceptionWithAttachmentWrapper("EA-219323", f(EA)).catch<IllegalStateException>()
}


fun main() {
    EA.ea141456 {
        it.vars("1", "2")
    }.invoke {
        System.out.println("1");
        throw IllegalStateException()
    }
}