// !LANGUAGE: -DataClassInheritance
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR

data class Foo(val s: String)

fun box(): String {
    val f1 = Foo("OK")
    val f2 = Foo("OK")
    if (f1 != f2) return "Fail equals"
    if (f1.hashCode() != f2.hashCode()) return "Fail hashCode"
    if (f1.toString() != f2.toString() || f1.toString() != "Foo(s=OK)") return "Fail toString: $f1 $f2"

    return f1.s
}
