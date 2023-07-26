inline fun <T, R> with(receiver: T, block: T.() -> R): R {/* ReanalyzableFunctionStructureElement */
    return receiver.block()
}

inline fun <T, R> T.let(block: (T) -> R): R {/* ReanalyzableFunctionStructureElement */
    return block(this)
}

class A {/* NonReanalyzableClassDeclarationStructureElement */
    fun foo() {/* ReanalyzableFunctionStructureElement */
        val a = with(1) {
            this.let { it }
        }.let { 2 }
    }
}
