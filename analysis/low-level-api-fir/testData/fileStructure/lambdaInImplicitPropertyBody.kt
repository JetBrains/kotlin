inline fun <T, R> with(receiver: T, block: T.() -> R): R {/* DeclarationStructureElement */
    return receiver.block()
}

inline fun <T, R> T.let(block: (T) -> R): R {/* DeclarationStructureElement */
    return block(this)
}

class B {/* ClassDeclarationStructureElement */
    val a: Int = 10/* DeclarationStructureElement */
    val x = with(a) {
        toString().let { it }
    }/* DeclarationStructureElement */
}
