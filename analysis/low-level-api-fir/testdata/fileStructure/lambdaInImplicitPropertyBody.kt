inline fun <T, R> with(receiver: T, block: T.() -> R): R {/* ReanalyzableFunctionStructureElement */
    return receiver.block()
}

inline fun <T, R> T.let(block: (T) -> R): R {/* ReanalyzableFunctionStructureElement */
    return block(this)
}

class B {/* NonReanalyzableClassDeclarationStructureElement */
    val a: Int = 10/* NonReanalyzableNonClassDeclarationStructureElement */
    val x = with(a) {
        toString().let { it }
    }/* NonReanalyzableNonClassDeclarationStructureElement */
}
