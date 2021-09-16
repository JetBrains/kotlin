// !LANGUAGE: +ContextReceivers

class A<T>(val a: T)
class B(val b: Any)
class C(val c: Any)

context(A<Int>, A<String>, B) fun f() {
    this@A.a.length
    this@B.b
    <!NO_THIS!>this<!>
}

context(A<Int>, A<String>, B) fun C.f() {
    this@A.a.length
    this@B.b
    this@C.c
    this@f.c
    this.c
}