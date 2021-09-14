inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    return receiver.block()
}

inline fun <T, R> T.let(block: (T) -> R): R {
    return block(this)
}

class A {
    fun foo() {
        val a = with(1) {
            this.let { it }
        }.let { 2 }
    }
}
