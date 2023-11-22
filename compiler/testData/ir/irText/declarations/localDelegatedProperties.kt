// WITH_STDLIB
// IGNORE_BACKEND: JS_IR

// KT-61141: `println (message: kotlin.Any?)` instead of `println (message: kotlin.Int)`
// KT-61141: `kotlin.collections.HashMap` instead of `java.util.HashMap`
// IGNORE_BACKEND: NATIVE

fun test1() {
    val x by lazy { 42 }
    println(x)
}

fun test2() {
    var x by hashMapOf<String, Int>()
    x = 0
    x++
    x += 1
}
