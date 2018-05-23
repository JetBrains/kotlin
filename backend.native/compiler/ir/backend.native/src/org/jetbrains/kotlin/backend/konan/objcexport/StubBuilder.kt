package org.jetbrains.kotlin.backend.konan.objcexport

internal class StubBuilder {
    private val children = mutableListOf<Stub<*>>()

    operator fun Stub<*>.unaryPlus() {
        children.add(this)
    }

    operator fun plusAssign(set: Collection<Stub<*>>) {
        children += set
    }

    fun build() = children
}

internal inline fun buildMembers(block: StubBuilder.() -> Unit): List<Stub<*>> = StubBuilder().let {
    it.block()
    it.build()
}
