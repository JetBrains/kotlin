// !LANGUAGE: +ContextReceivers

class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

context(A<String>) fun A<Int>.f() {
    this@A.a.length
}

<!CONFLICTING_OVERLOADS!>context(A<String>, B) fun f()<!> {
    this@A.a.length
    this@B.b
    <!NO_THIS!>this<!>
}

<!CONFLICTING_OVERLOADS!>context(A<Int>, A<String>, B) fun f()<!> {
    this@A.a.length
    this@B.b
    <!NO_THIS!>this<!>
}

context(A<Int>, A<String>, B) fun C.f() {
    this@A.a.length
    this@B.b
    this<!UNRESOLVED_LABEL!>@C<!>.c
    this@f.c
    this.c
}
