// FILE: soInlineUnitFun.kt
package soInlineUnitFun

fun main(args: Array<String>) {
    process()
}

fun process(): String {
    simple()
    //Breakpoint!
    return "Constant"             // 1
}

inline fun simple() {
    inner()
}

fun inner() {}
