import kotlin.*
import kotlin.collections.*

@CompileTimeCalculation
fun interface IntPredicate {
    fun accept(i: Int): Boolean

    fun defaultMethod() = 1
}

const val isEven = IntPredicate { it % 2 == 0 }
    .<!EVALUATED: `false, true, false, true, false`!>let { predicate -> listOf(1, 2, 3, 4, 5).map { predicate.accept(it) }.joinToString() }<!>
const val isOdd = IntPredicate { it % 2 != 0 }
    .<!EVALUATED: `true, false, true, false, true`!>let { predicate -> listOf(1, 2, 3, 4, 5).map { predicate.accept(it) }.joinToString() }<!>
const val callToDefault = IntPredicate { false }.<!EVALUATED: `1`!>defaultMethod()<!>
const val callToString = IntPredicate { false }.<!EVALUATED: `(kotlin.Int) -> kotlin.Boolean`!>toString()<!>

@CompileTimeCalculation
fun interface KRunnable {
    fun invoke(): String
}

@CompileTimeCalculation
object OK : () -> String {
    override fun invoke(): String = "OK"
}

@CompileTimeCalculation
fun foo(k: KRunnable) = k.invoke()

const val invokeFromObject = <!EVALUATED: `OK`!>foo(OK)<!>
const val invokeFromFunInterface = <!EVALUATED: `OK`!>foo(KRunnable { "OK" })<!>
