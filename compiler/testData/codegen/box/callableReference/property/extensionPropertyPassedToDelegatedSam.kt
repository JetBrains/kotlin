fun interface Base {
    fun print(a: String): String
}

val String.foo : String
    get() = this

class Derived(b: Base) : Base by b

fun box(): String {
    val a = Derived(Base(String::foo))
    with(a){
        return print("OK")
    }
}