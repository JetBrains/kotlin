// "Remove function body" "true"
abstract class A() {
    <caret>abstract fun foo() : Any = 1
}

/* IGNORE_FIR */