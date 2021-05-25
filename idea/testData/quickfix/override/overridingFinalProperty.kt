// "Make A.x open" "true"
open class A {
    final var x = 42;
}

class B : A() {
    override<caret> var x = 24;
}
/* IGNORE_FIR */
