// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

package regressions

fun f(xs: Iterator<Int>): Int {
    var answer = 0
    for (x in xs)  {
        answer += x
    }
    return answer
}

fun box(): String {
    val list = arrayListOf(1, 2, 3)
    val result = f(list.iterator())
    return if (6 == result) "OK" else "fail"
}
