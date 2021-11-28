fun <T> eval(fn: () -> T) = fn()

class A {
    public var prop = "OK"
        private set


    fun test(): String {
        return eval { prop }
    }
}

fun box(): String = A().test()