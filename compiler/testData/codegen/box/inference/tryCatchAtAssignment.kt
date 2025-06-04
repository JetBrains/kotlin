// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-77008

interface I {
    fun func(): String
}

class A : I {
    override fun func(): String = "OK"
}

class B : I {
    override fun func(): String ="Fail B"
}

fun <T> materialize(): T {
    return A() as T
}

class MyThrowable : Throwable("")

fun box(): String {
    val i: I
    // K1: OK
    // K2: Fails in Runtime with "class A cannot be cast to class B"
    i = try {
        materialize()
    } catch(e: MyThrowable) {
        B()
    }
    return i.func()
}
