// FILE: a.kt
package test

class A {}

// FILE: b.kt
package test.p; class C {fun f() {}}

// FILE: c.kt
package test.p; open class G<T> {open fun f(): T {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!> fun a() {}}

// FILE: d.kt
package test.p; class G2<E> : <!SUPERTYPE_NOT_INITIALIZED!>G<E><!> { fun g() : E {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!> override fun f() : E {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>}

// FILE: e.kt
package test.p; fun foo() {}

// FILE: f.kt
package test.p; fun foo(a: C) {}
