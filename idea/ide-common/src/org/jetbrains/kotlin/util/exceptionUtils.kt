/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments
import java.lang.IllegalStateException
import kotlin.reflect.KClass

class ExceptionWithAttachmentWrapper(val ref: String, vararg val vars: Any? = emptyArray()) {
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
                for (arg in vars.withIndex())
                    arg.value?.let {
                        exception.withAttachment("arg" + arg.index, arg.value.toString())
                    }
                throw exception
            }
            throw t;
        }
    }
}

object EA {
    fun ea141456(vararg vars: Any?) =
        ExceptionWithAttachmentWrapper("EA-141456", vars).catch<IllegalStateException>()

    fun ea219323(vararg vars: Any?) =
        ExceptionWithAttachmentWrapper("EA-219323", vars).catch<IllegalStateException>()
}


