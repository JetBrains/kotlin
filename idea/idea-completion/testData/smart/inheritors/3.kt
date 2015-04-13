trait T

open class A : T

class B : A()

fun foo(): T {
    open class Local1 : T

    class Local2 : Local1()

    return <caret>
}

fun x() {
    /*TODO*/
    /*class Local3 : T*/
}

open class C : T

class D : C()


// EXIST: A
// EXIST: B
// EXIST: Local1
// EXIST: Local2
// ABSENT: Local3
// EXIST: C
// EXIST: D
