import kotlin.*
import kotlin.collections.*

@CompileTimeCalculation
fun interface IntPredicate {
    fun accept(i: Int): Boolean

    fun defaultMethod() = 1
}

const val isEven = <!EVALUATED: `false, true, false, true, false`!>IntPredicate { it % 2 == 0 }
    .let { predicate -> listOf(1, 2, 3, 4, 5).map { predicate.accept(it) }.joinToString() }<!>
const val isOdd = <!EVALUATED: `true, false, true, false, true`!>IntPredicate { it % 2 != 0 }
    .let { predicate -> listOf(1, 2, 3, 4, 5).map { predicate.accept(it) }.joinToString() }<!>
const val callToDefault = <!EVALUATED: `1`!>IntPredicate { false }.defaultMethod()<!>
const val callToString = <!EVALUATED: `(kotlin.Int) -> kotlin.Boolean`!>IntPredicate { false }.toString()<!>

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
