// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
package test

annotation class Anno(val s: String)

const val C = "c"

class A @Anno("constructor" + C) constructor(@Anno("param" + C) i: Int = 42) {
    init {
        "body" + C
    }
}

typealias T<caret>A = A
