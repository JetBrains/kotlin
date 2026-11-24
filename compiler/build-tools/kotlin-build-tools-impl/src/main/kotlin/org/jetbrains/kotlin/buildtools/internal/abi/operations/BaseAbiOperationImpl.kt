/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.abi.operations

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.kotlin.abi.tools.AbiTools
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.internal.BuildOperationImpl
import org.jetbrains.kotlin.buildtools.internal.KotlinToolchainsImpl
import org.jetbrains.kotlin.buildtools.internal.daemon.AppendableVisitor
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.SerializedDataContainer
import org.jetbrains.kotlin.daemon.common.serialization.Message
import org.jetbrains.kotlin.daemon.common.serialization.MessageVisitor
import org.jetbrains.kotlin.daemon.common.serialization.SupportsNewDaemonMode
import org.jetbrains.kotlin.daemon.common.serialization.serialize

@Serializable
internal abstract class BaseAbiOperationImpl : BuildOperationImpl<Unit>, SupportsNewDaemonMode<Unit> {

    protected constructor(appendable: Appendable, abiTools: AbiTools = AbiTools.getInstance()) : super() {
        this.appendable = appendable
        this.abiTools = abiTools
    }

    protected constructor(other: BaseAbiOperationImpl) : super(other) {
        appendable = other.appendable
        abiTools = other.abiTools
    }

    @Transient
    protected var appendable: Appendable = PlaceholderAppendable

    @Transient
    protected var abiTools: AbiTools = AbiTools.getInstance()
        private set

    override fun beforeSerialization(logger: KotlinLogger): List<MessageVisitor> = buildList {
        addAll(super.beforeSerialization(logger))
        add(AppendableVisitor(appendable))
        appendable = PlaceholderAppendable
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

    protected object PlaceholderAppendable : Appendable {
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

