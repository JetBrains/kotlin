// FIR_IDENTICAL
// MODULE: m1
// FILE: m1a.kt
package a

class b
// FILE: m1b.kt
package c.d

// MODULE: m2()(m1)
// FILE: m2a.kt
package a.b

// FILE: m2b.kt
package c

class d
