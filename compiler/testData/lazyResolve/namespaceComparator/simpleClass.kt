// FILE: a.kt
class A {}

// FILE: b.kt
package p; class C {fun f() {}}

// FILE: c.kt
package p; open class G<T> {open fun f(): T {} fun a() {}}

// FILE: d.kt
package p; class G2<E> : G<E> { fun g() : E {} override fun f() : E {}}

// FILE: e.kt
package p; fun foo() {}

// FILE: f.kt
package p; fun foo(a: C) {}
