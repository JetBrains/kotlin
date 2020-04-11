// FLOW: IN
// WITH_RUNTIME

@file: JvmName("KotlinUtil")

import kotlin.jvm.JvmName

fun <caret>Any.extensionFun() {
}

fun String.foo() {
    "".extensionFun()

    1.extensionFun()

    extensionFun()

    with(123) {
        extensionFun()
    }
}

fun main() {
    "A".foo()
}

inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    return receiver.block()
}
