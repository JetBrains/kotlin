// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

class C {
    @kotlin.jvm.JvmOverloads public fun foo(s: String = "OK"): String {
        return s
    }
}

fun box(): String {
    val c = C()
    val m = c.javaClass.getMethod("foo")
    return m.invoke(c) as String
}
