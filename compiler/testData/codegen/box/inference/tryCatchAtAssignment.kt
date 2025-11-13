// ISSUE: KT-77008
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.0.0 2.1.0 2.2.0
// ^^^ KT-77008 is fixed in 2.3.0-Beta2

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
