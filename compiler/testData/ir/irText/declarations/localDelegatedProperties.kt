// WITH_STDLIB
// DUMP_IR_DIFFERENCE: JVM
//   K/JVM invokes `println (message: kotlin.Int)` instead of `println (message: kotlin.Any?)`
//   K/JVM uses actualized `java.util.HashMap` instead of `kotlin.collections.HashMap`

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
