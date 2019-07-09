// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun <K> select2(x: K, y: K): K = TODO()
fun <K> select3(x: K, y: K, z: K): K = TODO()

fun test2(f: ((String) -> Int)?) {
    val a0: ((Int) -> Int)? = select2({ it -> it }, null)
    val b0: ((Nothing) -> Unit)? = select2({ it -> <!UNUSED_EXPRESSION!>it<!> }, null)

    select3({ it.length }, f, null)
}
