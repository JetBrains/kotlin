// LL_FIR_DIVERGENCE
// KT-62861: Currently in LL FIR we don't have support for script files inside source roots
// LL_FIR_DIVERGENCE
// FILE: f1.kt
package test

class A
class F1

// FILE: A.kts
package test

val x = 1

class F1
