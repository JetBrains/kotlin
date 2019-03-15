// TARGET_BACKEND: JVM
// WITH_RUNTIME

class A {
    @JvmField val b = B()
}

class B {
    @JvmField val c = C()

    @JvmField val result = "OK"
}

class C {
    @JvmField var d = "Fail"
}

fun box(): String {
    val a = A()
    a.b.c.d = a.b.result
    return a.b.c.d
}
