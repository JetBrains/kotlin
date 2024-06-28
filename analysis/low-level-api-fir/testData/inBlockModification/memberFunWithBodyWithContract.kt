import kotlin.contracts.contract

class A {
    fun x() {
        contract {
            req
        }

        val a = <expr>doSmth("str")</expr>
    }
}

fun doSmth(i: String) = 4