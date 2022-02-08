// !LANGUAGE: +ContextReceivers

class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

<!MUST_BE_INITIALIZED!>context(A<String>, B) var <!REDECLARATION!>p<!>: Int<!>
    get() {
        this<!UNRESOLVED_LABEL!>@A<!>.a.length
        this<!UNRESOLVED_LABEL!>@B<!>.b
        <!NO_THIS!>this<!>
        return 1
    }
    set(value) {
        this<!UNRESOLVED_LABEL!>@A<!>.a.length
        this<!UNRESOLVED_LABEL!>@B<!>.b
        <!NO_THIS!>this<!>
        field = value
    }

<!MUST_BE_INITIALIZED!>context(A<Int>, A<String>, B) var <!REDECLARATION!>p<!>: Int<!>
    get() {
        this<!UNRESOLVED_LABEL!>@A<!>.a.toDouble()
        this<!UNRESOLVED_LABEL!>@A<!>.a.length
        this<!UNRESOLVED_LABEL!>@B<!>.b
        <!NO_THIS!>this<!>
        return 1
    }
    set(value) {
        this<!UNRESOLVED_LABEL!>@A<!>.a.length
        this<!UNRESOLVED_LABEL!>@B<!>.b
        <!NO_THIS!>this<!>
        field = value
    }

context(A<Int>, A<String>, B) val C.p: Int
    get() {
        this<!UNRESOLVED_LABEL!>@A<!>.a.length
        this<!UNRESOLVED_LABEL!>@B<!>.b
        this<!UNRESOLVED_LABEL!>@C<!>.c
        this@p.c
        this.c
        return 1
    }