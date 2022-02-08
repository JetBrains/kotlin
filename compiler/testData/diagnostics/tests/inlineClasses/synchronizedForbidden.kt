// TARGET_BACKEND: JVM
// WITH_STDLIB
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

    val f6
        <!SYNCHRONIZED_ON_VALUE_CLASS!>@Synchronized<!>
        get() = Unit

    val A.f7
        <!SYNCHRONIZED_ON_VALUE_CLASS!>@Synchronized<!>
        get() = Unit

    val String.f8
        <!SYNCHRONIZED_ON_VALUE_CLASS!>@Synchronized<!>
        get() = Unit
}

class Usual {

    @get:Synchronized
    val A.f9
        get() = Unit

    @Synchronized
    fun A.f10() = Unit

    val A.f11
        @Synchronized
        get() = Unit
}

@Synchronized
fun A.f12() = Unit

@get:Synchronized
val A.f13
    get() = Unit

val A.f14
    @Synchronized
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
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(block={}, lock='2')
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(block={}, lock=a)
    for (b in listOf(a)) {
        <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(b) {}
        <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(b.to(1).first) {}
        <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(block={}, lock=a)
    }
}
