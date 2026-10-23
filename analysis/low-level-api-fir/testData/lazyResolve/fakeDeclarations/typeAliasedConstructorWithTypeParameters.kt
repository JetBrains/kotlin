// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
package test

annotation class Anno(val s: String)

const val C = "c"

class A<T> @Anno("constructor" + C) constructor(@Anno("param" + C) t: T, s: String = "default" + C)

typealias T<caret>A = A<Int>
