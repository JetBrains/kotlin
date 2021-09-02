package org.jetbrains.kotlin.incremental.components

import org.jetbrains.kotlin.container.DefaultImplementation
import java.io.File

@DefaultImplementation(InlineConstTracker.DoNothing::class)
interface InlineConstTracker {
    fun report(className: String, cRefs: Collection<ConstantRef>)

    object DoNothing : InlineConstTracker {
        override fun report(className: String, cRefs: Collection<ConstantRef>) {
        }
    }
}

public data class ConstantRef(var owner: String, var name: String, var descriptor: String)
