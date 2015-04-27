package h

//traits to make ambiguity with function literal as an argument
trait A
trait B
trait C: A, B

fun foo<T>(<!UNUSED_PARAMETER!>a<!>: A, f: () -> T): T = f()
fun foo<T>(<!UNUSED_PARAMETER!>b<!>: B, f: () -> T): T = f()

fun test(c: C) {
    <!CANNOT_COMPLETE_RESOLVE!>foo<!>(c) f@ {
        c<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }
}
