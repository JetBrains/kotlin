// "Use inherited visibility" "true"
open class A {
    protected open fun run() {}
}

class B : A() {
    <caret>internal override fun run() {}
}

/* IGNORE_FIR */