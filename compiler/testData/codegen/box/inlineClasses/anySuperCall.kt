// WITH_STDLIB
// IGNORE_BACKEND: JVM

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val x: Int) {
    fun f(): Int = super.hashCode()
}

fun box(): String {
    val a = A(1).f()
    return "OK"
}
