// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// SKIP_TXT
// KT-49339
// FIR_IDENTICAL
// LANGUAGE: +ValueClasses

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

@JvmInline
value class B(val a: Int, val b: Int) {
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
    val B.f4
        get() = Unit

    <!SYNCHRONIZED_ON_VALUE_CLASS!>@Synchronized<!>
    fun B.f5() = Unit

    val f6
        <!SYNCHRONIZED_ON_VALUE_CLASS!>@Synchronized<!>
        get() = Unit

    val B.f7
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

    @get:Synchronized
    val B.f9
        get() = Unit

    @Synchronized
    fun B.f10() = Unit

    val B.f11
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

@Synchronized
fun B.f12() = Unit

@get:Synchronized
val B.f13
    get() = Unit

val B.f14
    @Synchronized
    get() = Unit

fun main() {
    val a = A(2)
    val b = B(3, 4)
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>a<!>) {}
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>2<!>) {}
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>0x2<!>) {}
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>2U<!>) {}
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>true<!>) {}
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>2L<!>) {}
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>2.to(1).first<!>) {}
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>2.toByte()<!>) {}
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>2UL<!>) {}
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>2F<!>) {}
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>2.0<!>) {}
    synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>'2'<!>) {}
    synchronized(block={}, <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>lock='2'<!>)
    synchronized(block={}, <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>lock=a<!>)
    for (a1 in listOf(a)) {
        synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>a1<!>) {}
        synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>a1.to(1).first<!>) {}
        synchronized(block={}, <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>lock=a1<!>)
    }
    for (b1 in listOf(b)) {
        synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>b1<!>) {}
        synchronized(<!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>b1.to(1).first<!>) {}
        synchronized(block={}, <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>lock=b1<!>)
    }
}
