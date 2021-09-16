// !LANGUAGE: +ContextReceivers

class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

context(labelAInt@A<Int>, A<String>, labelB@B) fun f() {
    this<!UNRESOLVED_LABEL!>@labelAInt<!>.a.toFloat()
    this<!UNRESOLVED_LABEL!>@A<!>.a.length
    this<!UNRESOLVED_LABEL!>@labelB<!>.b
    this<!UNRESOLVED_LABEL!>@B<!>
}

context(labelAInt@A<Int>, A<String>, labelB@B) val C.p: Int
    get() {
        this<!UNRESOLVED_LABEL!>@labelAInt<!>.a.toFloat()
        this<!UNRESOLVED_LABEL!>@A<!>.a.length
        this<!UNRESOLVED_LABEL!>@B<!>
        this<!UNRESOLVED_LABEL!>@labelB<!>.b
        this<!UNRESOLVED_LABEL!>@C<!>.c
        this@p.c
        this.c
        return 1
    }