// !DIAGNOSTICS: -MANY_COMPANION_OBJECTS -REDECLARATION -DUPLICATE_CLASS_NAMES

// KT-3464 Front-end shouldn't allow override modifier in class declaration

override class A {
    override companion <!REDECLARATION!>object<!> {}
    open companion <!MANY_COMPANION_OBJECTS, REDECLARATION!>object<!> {}
    abstract companion <!MANY_COMPANION_OBJECTS, REDECLARATION!>object<!> {}
    final companion <!MANY_COMPANION_OBJECTS, REDECLARATION!>object<!> {}
}

override object B1 {}
open object B2 {}
abstract object B3 {}
final object B4 {}

override enum class C {}
override interface D {}
override annotation class E