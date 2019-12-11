// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun <T> bar(i: T): T = i
fun foo(i: Int) = i

fun dontRun(body: () -> Unit) = Unit

class Case1 {
    fun test() {
        dontRun { val x = bar(bar { -> bar { -> 2} }) }
    }
}