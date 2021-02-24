// FLOW: IN
// RUNTIME_WITH_SOURCES

fun <caret>Any.extensionFun() {
}

fun String.foo() {
    with(123) {
        extensionFun()
    }

    with(456) {
        this.extensionFun()
    }

    with(789) {
        // no calls here
    }

    withNoInline(1) {
        extensionFun()
    }

    withNoInline(2) {
        // no calls here
    }

    "A".let {
        it.extensionFun()
    }

    "B".let {
        // no calls here
    }

    "D".letNoInline {
        it.extensionFun()
    }

    "C".letNoInline {
        // no calls here
    }
}

inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    val result = receiver.block()
    return result
}

fun <T, R> withNoInline(receiver: T, block: T.() -> R): R {
    val result = receiver.block()
    return result
}

fun <T, R> T.letNoInline(block: (T) -> R): R {
    return block(this)
}
