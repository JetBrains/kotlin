// !DIAGNOSTICS: -MANY_COMPANION_OBJECTS -REDECLARATION -DUPLICATE_CLASS_NAMES

// KT-3464 Front-end shouldn't allow override modifier in class declaration

override class A {
    override companion object {}
    open companion object {}
    abstract companion object {}
    final companion object {}
}

override object B1 {}
open object B2 {}
abstract object B3 {}
final object B4 {}

override enum class C {}
override interface D {}
override annotation class E