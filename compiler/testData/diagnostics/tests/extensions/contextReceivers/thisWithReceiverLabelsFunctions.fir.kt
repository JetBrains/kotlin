class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

context(A<Int>, A<String>, B) fun f() {
    this<!UNRESOLVED_LABEL!>@A<!>.a.length
    this<!UNRESOLVED_LABEL!>@B<!>.b
    <!NO_THIS!>this<!>
}

context(A<Int>, A<String>, B) fun C.f() {
    this<!UNRESOLVED_LABEL!>@A<!>.a.length
    this<!UNRESOLVED_LABEL!>@B<!>.b
    this<!UNRESOLVED_LABEL!>@C<!>.c
    this@f.c
    this.c
}