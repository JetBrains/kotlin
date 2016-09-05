// WITH_RUNTIME

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