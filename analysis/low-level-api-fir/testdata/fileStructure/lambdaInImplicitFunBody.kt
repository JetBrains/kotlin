inline fun <T, R> with(receiver: T, block: T.() -> R): R {/* ReanalyzableFunctionStructureElement */
    return receiver.block()
}

inline fun <T, R> T.let(block: (T) -> R): R {/* ReanalyzableFunctionStructureElement */
    return block(this)
}

class B {/* NonReanalyzableClassDeclarationStructureElement */
    fun foo(a: Int) = with(a) {
        toString().let { it }
    }/* NonReanalyzableNonClassDeclarationStructureElement */
}
