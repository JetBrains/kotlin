// FILE: a.kt
package test

class A {}

// FILE: b.kt
package test.p; class C {fun f() {}}

// FILE: c.kt
package test.p; open class G<T> {open fun f(): T {} fun a() {}}

// FILE: d.kt
package test.p; class G2<E> : G<E> { fun g() : E {} override fun f() : E {}}

// FILE: e.kt
package test.p; fun foo() {}

// FILE: f.kt
package test.p; fun foo(a: C) {}
