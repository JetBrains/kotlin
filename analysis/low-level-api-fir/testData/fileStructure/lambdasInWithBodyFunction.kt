inline fun <T, R> with(receiver: T, block: T.() -> R): R {/* DeclarationStructureElement */
    return receiver.block()
}

inline fun <T, R> T.let(block: (T) -> R): R {/* DeclarationStructureElement */
    return block(this)
}

class A {/* ClassDeclarationStructureElement */
    fun foo() {/* DeclarationStructureElement */
        val a = with(1) {
            this.let { it }
        }.let { 2 }
    }
}
