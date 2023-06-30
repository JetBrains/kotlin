import kotlin.contracts.contract

class A {
    fun <caret>x() {
        contract {
            req
        }

        val a = doSmth("str")
    }
}

fun doSmth(i: String) = 4