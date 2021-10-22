// TARGET_BACKEND: JVM
// WITH_RUNTIME
// SKIP_TXT
// KT-49339

@JvmInline
value class A(val a: Int) {
    <!SYNCHRONIZED_ON_VALUE_CLASS!>@get:Synchronized<!>
    val f0
        get() = Unit

    <!SYNCHRONIZED_ON_VALUE_CLASS!>@Synchronized<!>
    fun f1() = Unit

    <!SYNCHRONIZED_ON_VALUE_CLASS!>@Synchronized<!>
    fun String.f2() = Unit

    <!SYNCHRONIZED_ON_VALUE_CLASS!>@get:Synchronized<!>
    val String.f3
        get() = Unit

    <!SYNCHRONIZED_ON_VALUE_CLASS!>@get:Synchronized<!>
    val A.f4
        get() = Unit

    <!SYNCHRONIZED_ON_VALUE_CLASS!>@Synchronized<!>
    fun A.f5() = Unit
}

class Usual {

    @get:Synchronized
    val A.f6
        get() = Unit

    @Synchronized
    fun A.f7() = Unit

}

@Synchronized
fun A.f8() = Unit

@get:Synchronized
val A.f9
    get() = Unit

fun main() {
    val a = A(2)
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(a) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(2) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(0x2) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(2U) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(true) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(2L) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(2.to(1).first) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(2.toByte()) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(2UL) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(2F) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(2.0) {}
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>('2') {}
    for (b in listOf(a)) {
        <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(b) {}
        <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(b.to(1).first) {}
    }
}
