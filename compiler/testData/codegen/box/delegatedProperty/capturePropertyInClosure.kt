class Delegate {
    var inner = 1
    fun get(t: Any?, p: String): Int = inner
    fun set(t: Any?, p: String, i: Int) { inner = i }
}

class B {
    private var value: Int by Delegate()

    public fun test() {
        fun foo() {
            value = 1
        }
        foo()
    }
}

fun box(): String {
    B().test()
    return "OK"
}