// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

private object Case1 {
    interface Validator<in T>

    class CharSequenceValidator: Validator<CharSequence>

    class PredicateValidator<T>(val predicate: (T) -> Boolean): Validator<T>

    class CompositeValidator<T>(vararg val validators: Validator<T>)

    fun process(input: String) = true

    fun main() {
        val validators1 = CompositeValidator(CharSequenceValidator(), PredicateValidator(::process))
        val validators2 = CompositeValidator<String>(CharSequenceValidator(), PredicateValidator(::process))
        val validators3 = CompositeValidator(CharSequenceValidator(), PredicateValidator { it: String -> process(it) })
    }
}

private object Case2 {
    interface Expr<out T>
    data class Add(val left: Expr<Int>, val right: Expr<Int>): Expr<Int>
    data class Subtract(val left: Expr<Int>, val right: Expr<Int>): Expr<Int>

    fun f() {
        val operators1 = listOf(::Add, ::Subtract)
        val operators2 = listOf<(Expr<Int>, Expr<Int>) -> Expr<Int>>(::Add, ::Subtract)
    }

    fun <T> listOf(vararg elements: T): List<T> = TODO()
}
