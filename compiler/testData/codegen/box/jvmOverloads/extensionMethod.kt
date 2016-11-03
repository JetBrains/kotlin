// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

class C {
}

@kotlin.jvm.JvmOverloads fun C.foo(o: String, k: String = "K"): String {
    return o + k
}

fun box(): String {
    val m = C::class.java.getClassLoader().loadClass("ExtensionMethodKt").getMethod("foo", C::class.java, String::class.java)
    return m.invoke(null, C(), "O") as String
}
