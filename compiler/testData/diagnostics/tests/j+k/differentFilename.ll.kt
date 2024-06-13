// LL_FIR_DIVERGENCE
// two possible reasons:
// 1) LL FIR doesn't suffer from KT-4455 like standard Kotlin compiler does
// 2) LL FIR tests pass Java content roots to Kotlin compiler file-by-file instead of by a single folder
// LL_FIR_DIVERGENCE

// ISSUE: KT-4455
// SKIP_JAVAC

// FILE: A.java
public class A {
    public B b() {}
    public F f() {}
}

class B { public void bar() {} }

// FILE: C.java
class D {
    public void baz() {}
}

// FILE: E.java
class F {
    public void foobaz() {}
}

// FILE: main.kt
fun main(x: A) {
    x.b().bar()
    x.f().foobaz()

    D().baz()
}
