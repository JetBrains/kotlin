package lowlevel

annotation class Anno(val s: String)

const val prop = "str"

fun <T, U> function(t: T, u: U) where @Anno("declared $prop") U : Any, T : @Anno("bound $prop") Any {}

fun <T> functionWithUndeclared(t: T) where @Anno("undeclared $prop") AA : Any, @Anno("declared $prop") T : AA {}

fun functionWithoutTypeParameters() where @Anno("undeclared $prop") AA : Any {}

class Klass<T, U> where @Anno("declared $prop") U : Any, @Anno("undeclared $prop") AA : Any

val <T, U> Map<T, U>.property: Int where @Anno("declared $prop") U : Any, @Anno("undeclared $prop") AA : Any get() = 1
