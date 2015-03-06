//TODO: constructor modifiers are lost
a public class Annotations private [a] (private [a] val c1: Int, [a] val c2: Int) {

    protected a fun f() {
    }

    private fun annotationWithVararg(a vararg i: Int) {}

    b(E.E1) private val c: Int = 1

    a b(E.E2) public fun g([a] p1: E) {
    }

    var withCustomAccessors: Int = 0
    //TODO: accessor modifiers are lost
        [a] get
        [a] private set


    private b(E.E2) default object {

    }
}

annotation class a

annotation class b(val e: E)

enum class E { E1 E2 }