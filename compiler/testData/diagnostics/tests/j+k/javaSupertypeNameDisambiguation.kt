// FIR_IDENTICAL
// ISSUE: KT-64127

// FILE: DiffPackageBase.kt

package diff

abstract class Base {
    fun f() {}
}

// FILE: SamePackageBase.kt

abstract class Base

// FILE: Derived.java

import diff.Base;

public abstract class Derived extends Base {}

// FILE: Main.kt

class TestKlass: Derived() {
    fun test() {
        f()
    }
}

fun test(arg: Derived) {
    arg.f()
}
