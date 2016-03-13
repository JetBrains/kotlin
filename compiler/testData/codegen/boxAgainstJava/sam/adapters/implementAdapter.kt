// FILE: JavaInterface.java

interface JavaInterface {
    void foo(Runnable r);
}

// FILE: 1.kt

class Impl: JavaInterface {
    override fun foo(r: Runnable?) {
        r?.run()
    }
}

fun box(): String {
    val fooMethods = Impl::class.java.getMethods().filter { it.getName() == "foo" }
    if (fooMethods.size != 1) return fooMethods.toString()

    return "OK"
}
