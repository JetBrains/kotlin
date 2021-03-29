// "Use inherited visibility" "true"
open class A {
    protected open fun run() {}
}

class B : A() {
    <caret>private override fun run() {}
}

/* IGNORE_FIR */