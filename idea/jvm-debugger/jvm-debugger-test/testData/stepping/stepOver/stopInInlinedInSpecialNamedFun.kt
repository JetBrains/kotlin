package stopInInlinedInSpecialNamedFun

// KT-12470

class Foo(bar: Bar) {
    init {
        with(bar) {
            {
                //Breakpoint!
                nop()
                nop()
            }()
        }
    }

    fun nop() {}
}

fun main(args: Array<String>) {
    Foo(Bar())
}

class Bar

