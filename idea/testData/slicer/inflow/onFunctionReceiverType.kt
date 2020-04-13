// FLOW: IN
// RUNTIME_WITH_SOURCES

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
