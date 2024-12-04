inline fun <T, R> with(receiver: T, block: T.() -> R): R {/* DeclarationStructureElement */
    return receiver.block()
}

inline fun <T, R> T.let(block: (T) -> R): R {/* DeclarationStructureElement */
    return block(this)
}

class B {/* ClassDeclarationStructureElement */
    fun foo(a: Int) = with(a) {
        toString().let { it }
    }/* DeclarationStructureElement */
}
