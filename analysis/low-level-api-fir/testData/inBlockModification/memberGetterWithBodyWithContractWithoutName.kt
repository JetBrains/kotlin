import kotlin.contracts.contract

class A {
    var : Int
    get() {
        contract {
            req
        }

        fun <expr>doSmth</expr>(i: String) = 4
        return doSmth("str")
    }
    set(value) = Unit
}
