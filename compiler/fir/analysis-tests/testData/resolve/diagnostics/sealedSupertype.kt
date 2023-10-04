sealed class A

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!> : A()

interface C : <!INTERFACE_WITH_SUPERCLASS!>A<!>

interface D : C, <!INTERFACE_WITH_SUPERCLASS!>A<!>

class E : <!FINAL_SUPERTYPE!>B<!>, <!MANY_CLASSES_IN_SUPERTYPE_LIST!>A<!>()

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

object <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!> {
    class I : P()
}

fun test() {
    class L : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>P<!>()
    val a = object : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>P<!>() {

    }
}
