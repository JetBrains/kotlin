// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect open class Container {
    internal fun internalFun()
}

// MODULE: m2-jvm()()(m1-common)

// FILE: foo/Foo.java

package foo;

public class Foo {
    public final void internalFun() {}    // OK: internal -> public
}

// FILE: jvm.kt

actual typealias Container = foo.Foo
