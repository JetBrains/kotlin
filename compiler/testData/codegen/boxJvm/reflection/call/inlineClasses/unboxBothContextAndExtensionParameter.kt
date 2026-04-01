// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +ContextParameters

@JvmInline
value class X(val x: String)

@JvmInline
value class Y(val y: Char)

class C {
    context(x: X)
    fun Y.f(): String = x.x + y
}

fun box(): String {
    val f = C::class.members.single { it.name == "f" }
    return f.call(C(), X("O"), Y('K')) as String
}
