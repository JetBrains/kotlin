open class Base {
    open val <T> (Int.()-> T).bar: T
        get() = this(1)
    open fun <T> (Int.()-> T).foo(): T { return this(1) }
}

class A : Base() {
    fun test(): String {
        val a = fun Int.():String { return "O" }.bar
        val b = fun Int.():String { return "K" }.foo()
        return a+b
    }
}

fun box(): String {
    return A().test()
}