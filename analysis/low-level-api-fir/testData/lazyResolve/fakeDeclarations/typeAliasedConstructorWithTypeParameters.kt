// IGNORE_FIR
// KT-73481: the lazy body calculation revives the substitution override instead of the original constructor
// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
package test

annotation class Anno(val s: String)

const val C = "c"

class A<T> @Anno("constructor" + C) constructor(@Anno("param" + C) t: T, s: String = "default" + C)

typealias T<caret>A = A<Int>
