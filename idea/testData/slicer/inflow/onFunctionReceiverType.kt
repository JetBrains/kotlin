// FLOW: IN

@file: JvmName("KotlinUtil")

import kotlin.jvm.JvmName

fun <caret>Any.extensionFun() {
}

fun String.foo() {
    "".extensionFun()

    1.extensionFun()

    extensionFun()
}

fun main() {
    "A".foo()
}
