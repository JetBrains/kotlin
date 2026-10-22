/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.KotlinToolchainsImpl
import org.jetbrains.kotlin.buildtools.internal.serializability.*
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.SerializedDataContainer

@Serializable
internal abstract class BaseAbiOperationImpl : BuildOperationImpl<Unit>(), SupportsNewDaemonMode<Unit> {

    protected abstract var appendable: Appendable

    override fun beforeSerialization(logger: KotlinLogger): List<MessageVisitor> = buildList {
        addAll(super.beforeSerialization(logger))
        add(AppendableVisitor(appendable))
        appendable = DoNothingAppendable
    }

    override fun afterDeserialization(messageReporter: (Message) -> Unit, compilerFacade: CompilerServicesFacadeBase?) {
        super.afterDeserialization(messageReporter, compilerFacade)
        appendable = object : Appendable {
            override fun append(csq: CharSequence?): java.lang.Appendable {
                messageReporter(Message.Appendable.Append(csq.toString()))
                return this
            }

            override fun append(csq: CharSequence?, start: Int, end: Int): java.lang.Appendable {
                messageReporter(Message.Appendable.Append(csq?.subSequence(start, end)?.toString() ?: csq.toString()))
                return this
            }

            override fun append(c: Char): java.lang.Appendable {
                messageReporter(Message.Appendable.Append(c.toString()))
                return this
            }
        }
    }

    override fun serializeResult(result: Unit): SerializedDataContainer {
        return KotlinToolchainsImpl.serializer.serialize(result)
    }

    override fun deserializeResult(serializedData: SerializedDataContainer) {
        return
    }

    override val usesApplicationEnvironment: Boolean
        get() = false

    protected object DoNothingAppendable : Appendable {
        override fun append(csq: CharSequence?): java.lang.Appendable? {
            error("Not implemented")
        }

        override fun append(csq: CharSequence?, start: Int, end: Int): java.lang.Appendable? {
            error("Not implemented")
        }

        override fun append(c: Char): java.lang.Appendable? {
            error("Not implemented")
        }
    }
}

