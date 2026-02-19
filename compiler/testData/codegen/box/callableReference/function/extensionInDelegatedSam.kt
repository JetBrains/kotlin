fun interface Base {
    fun String.print(): String
}

fun foo(a: String): String { return a }

class Derived(b: Base) : Base by b

fun box(): String {
    val a = Derived(Base(::foo))
    with(a){
        return "OK".print()
    }
}