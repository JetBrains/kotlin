interface Base {
    fun print()
}

class BaseImpl(val x: Int) : Base {
    override fun print() {
        print(x)
    }
}

class Derived(b: Base) : Base by b

fun box(): String {
    val b = BaseImpl(10)
    Derived(b).print()
    return "OK"
}