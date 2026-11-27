/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.utils.pollFirst

class IrLinkerDeserializationQueue {
    private val queue = LinkedHashSet<IrModuleDeserializationQueue>()

    fun enqueue(moduleQueue: IrModuleDeserializationQueue) {
        queue += moduleQueue
    }

    fun drain() {
        while (true) {
            val child: IrModuleDeserializationQueue = queue.firstOrNull() ?: break
            child.drain()
            queue.remove(child)
        }
    }
}

sealed class IrModuleDeserializationQueue(protected val parent: IrLinkerDeserializationQueue) {
    abstract fun drain()
}

class IrModuleWithFilesDeserializationQueue(parent: IrLinkerDeserializationQueue) : IrModuleDeserializationQueue(parent) {
    private val queue = LinkedHashSet<IrFileDeserializationQueue>()

    fun enqueue(fileQueue: IrFileDeserializationQueue) {
        if (queue.add(fileQueue)) {
            parent.enqueue(this)
        }
    }

    override fun drain() {
        while (true) {
            val child: IrFileDeserializationQueue = queue.firstOrNull() ?: break
            child.drain()
            queue.remove(child)
        }
    }
}

abstract class IrModuleWithoutFilesDeserializationQueue(parent: IrLinkerDeserializationQueue) : IrModuleDeserializationQueue(parent) {
    fun enqueueFullDeserializationOfModule() {
        parent.enqueue(this)
    }
}

abstract class IrFileDeserializationQueue(private val parent: IrModuleWithFilesDeserializationQueue) {
    private val queue = LinkedHashSet<SignatureAndKind>()
    private var fileIsProcessed = false

    fun enqueueInBulk(signatureAndKinds: Collection<SignatureAndKind>, doNotEnqueueFile: Boolean = false) {
        queue += signatureAndKinds
        if (signatureAndKinds.isNotEmpty() && !doNotEnqueueFile) {
            parent.enqueue(this)
        }
    }

    fun enqueue(signatureAndKind: SignatureAndKind) {
        queue += signatureAndKind
        parent.enqueue(this)
    }

    fun drain() {
        if (!fileIsProcessed) {
            fileIsProcessed = true
            processFileOnce()
        }

        while (true) {
            val signatureAndKind = queue.pollFirst() ?: break
            deserializeDeclaration(signatureAndKind)
        }
    }

    protected abstract fun processFileOnce()

    protected abstract fun deserializeDeclaration(signatureAndKind: SignatureAndKind)
}
