// FILE: a.kt
package a
interface A

// FILE: b.kt
package b
interface B : a.A

class C : B

// LIGHT_CLASS_FQ_NAME: b.B, b.C
