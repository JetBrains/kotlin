// !LANGUAGE: +ContextReceivers

class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

context(A<Int>, A<String>, B) var p: Int
    get() {
        this@A.a.<!UNRESOLVED_REFERENCE!>toDouble<!>()
        this@A.a.length
        this@B.b
        <!NO_THIS!>this<!>
        return 1
    }
    set(value) {
        this@A.a.length
        this@B.b
        <!NO_THIS!>this<!>
        <!UNRESOLVED_REFERENCE!>field<!> = value
    }

context(A<Int>, A<String>, B) val C.p: Int
    get() {
        this@A.a.length
        this@B.b
        this@C.c
        this@p.c
        this.c
        return 1
    }