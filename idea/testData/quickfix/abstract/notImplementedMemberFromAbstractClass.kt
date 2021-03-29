// "Make 'B' 'abstract'" "true"
abstract class A {
    abstract fun foo()
}

<caret>class B : A() {
}

/* IGNORE_FIR */