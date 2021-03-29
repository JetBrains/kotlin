// "Remove function body" "true"
abstract class A() {
    <caret>abstract fun foo() : Any { return "a" }
}

/* IGNORE_FIR */