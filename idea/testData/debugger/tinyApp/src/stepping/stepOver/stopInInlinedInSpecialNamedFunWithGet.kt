package stopInInlinedInSpecialNamedFunWithGet

// KT-12470

class Foo(bar: Bar) {
    init {
        with(bar) {
            get<String> {
                //Breakpoint!
                nop()
                nop()
            }
        }
    }

    private fun nop() {}
}

fun main(args: Array<String>) {
    Foo(Bar())
}

class Bar
class BarContext

inline fun <reified T : Any> Bar.get(noinline body: BarContext.() -> Unit) {
    BarContext().body()
}
