sealed class A

<!REDECLARATION!>class B : A()<!>

interface C : <!INTERFACE_WITH_SUPERCLASS!>A<!>

interface D : C, <!INTERFACE_WITH_SUPERCLASS!>A<!>

class E : B, A()

sealed class P {
    object H: P()
    class J : P()

    object T {
        object V : P()
        class M : P()
    }

    val p: P = object : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>P<!>() {

    }

    val r = object : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>P<!>() {

    }
}

class K : P()

<!REDECLARATION!>object B<!> {
    class I : <!SEALED_SUPERTYPE!>P<!>()
}

fun test() {
    class L : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>P<!>()
    val a = object : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>P<!>() {

    }
}