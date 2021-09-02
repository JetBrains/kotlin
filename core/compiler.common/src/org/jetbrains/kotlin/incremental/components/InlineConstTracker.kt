package org.jetbrains.kotlin.incremental.components

import org.jetbrains.kotlin.container.DefaultImplementation
import java.io.Serializable

@DefaultImplementation(InlineConstTracker.DoNothing::class)
interface InlineConstTracker {
    fun report(filePath: String, cRefs: Collection<ConstantRef>)

    object DoNothing : InlineConstTracker {
        override fun report(filePath: String, cRefs: Collection<ConstantRef>) {
        }
    }
}

data class ConstantRef(var owner: String, var name: String, var constType: String) : Serializable
