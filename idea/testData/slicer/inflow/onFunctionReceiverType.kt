// FLOW: IN

@file: JvmName("KotlinUtil")

import kotlin.jvm.JvmName

fun <caret>Any.extensionFun() {
}

fun foo() {
    "".extensionFun()
    1.extensionFun()
}