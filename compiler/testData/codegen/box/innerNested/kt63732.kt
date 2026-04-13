// IGNORE_BACKEND: ANDROID
// ^KT-63732

lateinit var foo: Any

class A<T : Any> {
    inner class B(x: T) {
        init {
            foo = x
        }
    }

    fun foo(t: T) {
        object {
            val something: B = B(t)
        }
    }
}

fun box(): String {
    A<String>().foo("OK")
    return foo as String
}
