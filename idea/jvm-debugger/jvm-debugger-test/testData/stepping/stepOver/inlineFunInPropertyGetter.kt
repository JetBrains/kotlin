package inlineFunInPropertyGetter

fun main() {
    Goo().f
}

class Foo(val a: String?)

class DumbList<T>(private val element: T) : AbstractList<T>() {
    override val size get() = 1
    override fun get(index: Int) = if (index == 0) element else error("Invalid index $index")
}

class Goo {
    val f: Int
        get() {
            //Breakpoint!
            val a = DumbList(Foo("a"))
            val b = a.mapNotNull { it.a }
            return 1
        }
}

// STEP_OVER: 2