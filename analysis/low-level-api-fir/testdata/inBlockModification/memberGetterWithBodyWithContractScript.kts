import kotlin.contracts.contract

class A {
    var x: Int
    get() {
        contract {
            req
        }

        fun <expr>doSmth</expr>(i: String) = 4
        return doSmth("str")
    }
    set(value) = Unit
}
