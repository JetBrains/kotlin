// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_TXT
// KT-49339
// LANGUAGE: +ProhibitSynchronizationByValueClassesAndPrimitives

@JvmInline
value class A(val a: Int) {
    @get:Synchronized
    val f0
        get() = Unit

    @Synchronized
    fun f1() = Unit

    @Synchronized
    fun String.f2() = Unit

    @get:Synchronized
    val String.f3
        get() = Unit

    @get:Synchronized
    val A.f4
        get() = Unit

    @Synchronized
    fun A.f5() = Unit

    val f6
        @Synchronized
        get() = Unit

    val A.f7
        @Synchronized
        get() = Unit

    val String.f8
        @Synchronized
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
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>a<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>2<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>0x2<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>2U<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>true<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>2L<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>2.to(1).first<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>2.toByte()<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>2UL<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>2F<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>2.0<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>'2'<!>) {}
    synchronized(block={}, lock=<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>'2'<!>)
    synchronized(block={}, lock=<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>a<!>)
    for (b in listOf(a)) {
        synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>b<!>) {}
        synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>b.to(1).first<!>) {}
        synchronized(block={}, lock=<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>a<!>)
    }
}
