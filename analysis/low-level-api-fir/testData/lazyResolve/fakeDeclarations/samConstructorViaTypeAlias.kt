// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
package test

annotation class Anno(val s: String)

const val C = "c"

fun interface I<@Anno("typeParameter" + C) T> {
    @Anno("fun" + C)
    fun run(t: T): String
}

typealias T<caret>I = I<Int>
