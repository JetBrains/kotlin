// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_HMPP: JVM_IR

// MODULE: lib-common
// FILE: lib-common.kt
expect open class A()

open class B : A()

// MODULE: lib-platform()()(lib-common)
// FILE: lib-platform.kt
actual open class A actual constructor()

class C : B()

// MODULE: app-common(lib-common)
// FILE: app-common.kt
class D : B()

// MODULE: app-platform(lib-platform)()(app-common)
// FILE: app-platform.kt
class E : B()

fun box(): String {
    val a = A()
    a.equals(a)
    val b = B()
    b.equals(b)
    val c = C()
    c.equals(c)
    val d = D()
    d.equals(d)
    val e = E()
    e.equals(e)
    return "OK"
}
