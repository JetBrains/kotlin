@file:Suppress("RedundantSuspendModifier", "NOTHING_TO_INLINE")

class Cache {
    private val cache = mutableMapOf<String, String>()
    fun load(key: String): String? = cache[key]
    fun store(key: String, value: String) { cache[key] = value }
    fun dumpToString(): String = cache.entries.sortedBy { it.key }.joinToString(",") { it.key + "=" + it.value }
}

class OperatorsToNonOperators(private val cache: Cache) {
    operator fun get(key: String): String? = cache.load(key)
    operator fun set(key: String, value: String) = cache.store(key, value)
    operator fun invoke(): String = cache.dumpToString()

    companion object {
        operator fun Cache.get(key: String): String? = load(key)
        operator fun Cache.set(key: String, value: String) = store(key, value)
        operator fun Cache.invoke(): String = dumpToString()
    }
}

class NonOperatorsToOperators(private val cache: Cache) {
    fun get(key: String): String? = cache.load(key)
    fun set(key: String, value: String) = cache.store(key, value)
    fun invoke(): String = cache.dumpToString()

    companion object {
        fun Cache.get(key: String): String? = load(key)
        fun Cache.set(key: String, value: String) = store(key, value)
        fun Cache.invoke(): String = dumpToString()
    }
}

data class Wrapper(private val value: Int) {
    private operator fun plus(other: Wrapper): Wrapper = (value + other.value).wrap()
    fun unwrap(): Int = value

    fun memberNonInfixToInfix(other: Wrapper): Wrapper = this + other
    infix fun memberInfixToNonInfix(other: Wrapper): Wrapper = this + other

    companion object {
        fun Wrapper.extensionNonInfixToInfix(other: Wrapper): Wrapper = this + other
        infix fun Wrapper.extensionInfixToNonInfix(other: Wrapper): Wrapper = this + other
    }
}

fun Int.wrap(): Wrapper = Wrapper(this)

object Functions {
    fun nonTailrecToTailrec(n: Int, r: Int): Int = if (n <= 1) r else nonTailrecToTailrec(n - 1, n * r)
    tailrec fun tailrecToNonTailrec(n: Int, r: Int): Int = if (n <= 1) r else tailrecToNonTailrec(n - 1, n * r)

    @Suppress("RedundantSuspendModifier") suspend fun <R> wrapCoroutine(coroutine: suspend () -> R): R = coroutine.invoke()
    suspend fun suspendToNonSuspendFunction(x: Int): Int = wrapCoroutine { -x }
    fun nonSuspendToSuspendFunction(x: Int): Int = -x

    inline fun inlineLambdaToNoinlineLambda(x: Int, lambda: (Int) -> String): String = "Functions.inlineLambdaToNoinlineLambda($x) { ${lambda(x * 2)} }"
    inline fun inlineLambdaToCrossinlineLambda(x: Int, lambda: (Int) -> String): String = "Functions.inlineLambdaToCrossinlineLambda($x) { ${lambda(x * 2)} }"

    fun removedFirstDefaultValue(a: Int = 42, b: Int): Int = a + b
    fun removedVarargFirstDefaultValue(vararg a: Int = intArrayOf(1, 2, 3), b: Int): Int = a.sum() + b
    fun removedLastDefaultValue(a: Int, b: Int = 42): Int = a + b
    fun removedVarargLastDefaultValue(a: Int, vararg b: Int = intArrayOf(1, 2, 3)): Int = a + b.sum()
}

class RemovedFirstDefaultValueInConstructor(a: Int = 42, b: Int) {
    val value = a + b
}
class RemovedLastDefaultValueInConstructor(a: Int, b: Int = 42) {
    val value = a + b
}

interface Interface {
    suspend fun suspendToNonSuspendFunction(x: Int): String
    fun nonSuspendToSuspendFunction(x: Int): String
}

abstract class AbstractClass {
    abstract suspend fun suspendToNonSuspendFunction(x: Int): String
    abstract fun nonSuspendToSuspendFunction(x: Int): String
}

open class OpenClass {
    open suspend fun suspendToNonSuspendFunction(x: Int): String = Functions.wrapCoroutine { "OpenClass.suspendToNonSuspendFunction($x)" }
    open fun nonSuspendToSuspendFunction(x: Int): String = "OpenClass.nonSuspendToSuspendFunction($x)"

    open suspend fun suspendToNonSuspendFunctionWithDelegation(x: Int): String = Functions.wrapCoroutine { "OpenClass.suspendToNonSuspendFunctionWithDelegation($x)" }
    open fun nonSuspendToSuspendFunctionWithDelegation(x: Int): String = "OpenClass.nonSuspendToSuspendFunctionWithDelegation($x)"

    open fun openNonInlineToInlineFunction(x: Int): String = "OpenClass.openNonInlineToInlineFunction($x)"
    open fun openNonInlineToInlineFunctionWithDelegation(x: Int): String = "OpenClass.openNonInlineToInlineFunctionWithDelegation($x)"
    //inline fun newInlineFunction1(x: Int): String = "OpenClass.newInlineFunction1($x)"
    //inline fun newInlineFunction2(x: Int): String = "OpenClass.newInlineFunction2($x)"
    //fun newNonInlineFunction(x: Int): String = "OpenClass.newNonInlineFunction($x)"

    fun newInlineFunction1Caller(x: Int): String = TODO("Not implemented: OpenClass.newInlineFunction1Caller($x)")
    fun newInlineFunction2Caller(x: Int): String = TODO("Not implemented: OpenClass.newInlineFunction2Caller($x)")
    fun newNonInlineFunctionCaller(x: Int): String = TODO("Not implemented: OpenClass.newNonInlineFunctionCaller($x)")
}
