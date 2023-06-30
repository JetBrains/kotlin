import kotlin.contracts.contract

class A {
    var x: Int
    ge<caret>t() {
        contract {
            req
        }

        fun doSmth(i: String) = 4
        return doSmth("str")
    }
    set(value) = Unit
}
