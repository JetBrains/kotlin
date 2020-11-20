// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
// KT-3525
object B {
    <!REDECLARATION!>class C<!>
    <!REDECLARATION!>class C<!>

    <!REDECLARATION!>val a : Int = 1<!>
    <!REDECLARATION!>val a : Int = 1<!>
}