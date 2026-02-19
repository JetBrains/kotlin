fun interface Base {
    fun String.print(): String
}

val String.foo : String
    get() = this

class Derived(b: Base) : Base by b

fun box(): String {
    val a = Derived(Base(String::foo))
    with(a){
        return "OK".print()
    }
}