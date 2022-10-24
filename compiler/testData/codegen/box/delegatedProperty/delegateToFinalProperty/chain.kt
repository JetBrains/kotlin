// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

class A {
    val b = B()
}

class B {
    val c = C()
}

class C {
    val d = D()
}

class D {
    val e = 1
}

val a = A()

operator fun Int.getValue(thisRef: Any?, property: Any?) =
    if (this == 1 && thisRef == null) "OK" else "Failed"

val x by a.b.c.d.e

fun box() = x
