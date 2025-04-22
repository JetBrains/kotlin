// IGNORE_NATIVE: compatibilityTestMode=FORWARD
// ^^^ This new test fails under 2.1.0 compiler with IndexOutOfBoundsException and passes on 2.2.0 and later
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