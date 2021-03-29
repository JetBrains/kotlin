// "Remove function body" "true"
abstract class A() {
    <caret>abstract fun foo() = /*1*/
            { "" /*2*/ } // 3
}

/* IGNORE_FIR */