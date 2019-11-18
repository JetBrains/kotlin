// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Foo(val x: Int) : Comparable<Foo> {
    override fun compareTo(other: Foo): Int {
        return 10
    }
}

fun box(): String {
    val f1 = Foo(42)
    val ff1: Comparable<Foo> = f1

    if (ff1.compareTo(f1) != 10) return "Fail"

    return "OK"
}