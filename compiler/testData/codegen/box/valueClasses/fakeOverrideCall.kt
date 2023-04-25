// !LANGUAGE: +ValueClasses
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING


@JvmInline
value class DPoint(val x: Double, val y: Double)

class A : B()

class C {
    fun set(value: DPoint) = A().set(value)
}

open class B {

    fun set(value: DPoint) = "OK"
}

fun box(): String = A().set(DPoint(1.0, 2.0))
