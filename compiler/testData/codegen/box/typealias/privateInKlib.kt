// MODULE: lib
// FILE: f1.kt

private typealias TA = String

public interface I {
    public fun foo(): TA
}

open public class B: I {
    override fun foo() = "OK"
}

// FILE: f2.kt


public class D: B()

// MODULE: main(lib)
// FILE: m.kt

fun box() = D().foo()