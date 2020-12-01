// ISSUE: KT-13495
// IGNORE_BACKEND_FIR: JVM_IR
// !LANGUAGE: +AllowSealedInheritorsInDifferentFilesOfSamePackage

// FILE: Base.kt

sealed class Base {
    class A : Base()
}

// FILE: B.kt

class B : Base()

// FILE: Container.kt

class Containter {
    class C : Base()

    inner class D : Base()

    val d = D()
}

// FILE: main.kt

fun getValue(base: Base): Int = when (base) {
    is Base.A -> 1
    is B -> 2
    is Containter.C -> 3
    is Containter.D -> 4
}

fun box(): String {
    var res = 0
    res += getValue(Base.A())
    res += getValue(B())
    res += getValue(Containter.C())
    res += getValue(Containter().d)
    return if (res == 10) "OK" else "Fail"
}
