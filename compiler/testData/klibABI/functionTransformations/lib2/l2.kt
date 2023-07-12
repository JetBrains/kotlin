import kotlin.coroutines.*

// Auxiliary function to imitate coroutines.
private fun <R> runCoroutine(coroutine: suspend () -> R): R {
    var coroutineResult: Result<R>? = null

    coroutine.startCoroutine(Continuation(EmptyCoroutineContext) { result ->
        coroutineResult = result
    })

    return (coroutineResult ?: error("Coroutine finished without any result")).getOrThrow()
}

private inline fun <R> runInlined(block: () -> R): R = block() // a-la kotlin.run() but without contracts and special annotation

fun memberOperatorsToNonOperators(vararg pairs: Pair<String, String>): String {
    check(pairs.isNotEmpty())
    val instance = OperatorsToNonOperators(Cache())
    pairs.forEach { (key, value) ->
        instance[key] = value // set
    }
    pairs.forEach { (key, value) ->
        check(instance[key] == value) // get
    }
    return "memberOperatorsToNonOperators: " + instance() // invoke
}

fun extensionOperatorsToNonOperators(vararg pairs: Pair<String, String>): String = with(OperatorsToNonOperators.Companion) {
    check(pairs.isNotEmpty())
    val cache = Cache()
    pairs.forEach { (key, value) ->
        cache[key] = value // set
    }
    pairs.forEach { (key, value) ->
        check(cache[key] == value) // get
    }
    return "extensionOperatorsToNonOperators: " + cache() // invoke
}

fun memberNonOperatorsToOperators(vararg pairs: Pair<String, String>): String {
    check(pairs.isNotEmpty())
    val instance = NonOperatorsToOperators(Cache())
    pairs.forEach { (key, value) ->
        instance.set(key, value) // set
    }
    pairs.forEach { (key, value) ->
        check(instance.get(key) == value) // get
    }
    return "memberNonOperatorsToOperators: " + instance.invoke() // invoke
}

fun extensionNonOperatorsToOperators(vararg pairs: Pair<String, String>): String = with(NonOperatorsToOperators.Companion) {
    check(pairs.isNotEmpty())
    val cache = Cache()
    pairs.forEach { (key, value) ->
        cache.set(key, value) // set
    }
    pairs.forEach { (key, value) ->
        check(cache.get(key) == value) // get
    }
    return "extensionNonOperatorsToOperators: " + cache.invoke() // invoke
}

fun memberNonInfixToInfix(a: Int, b: Int): Int = a.wrap().memberNonInfixToInfix(b.wrap()).unwrap()
fun extensionNonInfixToInfix(a: Int, b: Int): Int = with(Wrapper.Companion) { a.wrap().extensionNonInfixToInfix(b.wrap()).unwrap() }
fun memberInfixToNonInfix(a: Int, b: Int): Int = (a.wrap() memberInfixToNonInfix b.wrap()).unwrap()
fun extensionInfixToNonInfix(a: Int, b: Int): Int = with(Wrapper.Companion) { (a.wrap() extensionInfixToNonInfix b.wrap()).unwrap() }

fun nonTailrecToTailrec(n: Int): Int = Functions.nonTailrecToTailrec(n, 1)
@Suppress("NO_TAIL_CALLS_FOUND") tailrec fun tailrecToNonTailrec(n: Int): Int = Functions.tailrecToNonTailrec(n, 1)

// This is required to check that default arguments are counter correctly even for inherited classes.
open class StableOpenClass {
    open fun firstDefaultValueInFunction(a: Int = 42, b: Int): Int = a + b
    open fun lastDefaultValueInFunction(a: Int, b: Int = 42): Int = a + b
}
open class StableClassImpl : StableOpenClass()
class StableClassImpl2 : StableClassImpl() {
    override fun firstDefaultValueInFunction(a: Int, b: Int): Int = a - b
    override fun lastDefaultValueInFunction(a: Int, b: Int): Int = a - b
}

fun firstDefaultValueInFunctionInStableOpenClass(stableOpenClass: StableOpenClass, n: Int): Int = stableOpenClass.firstDefaultValueInFunction(b = n)
fun lastDefaultValueInFunctionInStableOpenClass(stableOpenClass: StableOpenClass, n: Int): Int = stableOpenClass.lastDefaultValueInFunction(a = n)
fun firstDefaultValueInFunctionInStableClassImpl(stableClassImpl: StableClassImpl, n: Int): Int = stableClassImpl.firstDefaultValueInFunction(b = n)
fun lastDefaultValueInFunctionInStableClassImpl(stableClassImpl: StableClassImpl, n: Int): Int = stableClassImpl.lastDefaultValueInFunction(a = n)
fun firstDefaultValueInFunctionInStableClassImpl2(stableClassImpl2: StableClassImpl2, n: Int): Int = stableClassImpl2.firstDefaultValueInFunction(b = n)
fun lastDefaultValueInFunctionInStableClassImpl2(stableClassImpl2: StableClassImpl2, n: Int): Int = stableClassImpl2.lastDefaultValueInFunction(a = n)

fun removedFirstDefaultValueInFunction(n: Int): Int = Functions.removedFirstDefaultValue(b = n)
fun removedVarargFirstDefaultValueInFunction(n: Int): Int = Functions.removedVarargFirstDefaultValue(b = n)
fun removedLastDefaultValueInFunction(n: Int): Int = Functions.removedLastDefaultValue(a = n)
fun removedVarargLastDefaultValueInFunction(n: Int): Int = Functions.removedVarargLastDefaultValue(a = n)
fun removedFirstDefaultValueInConstructor(n: Int): Int = RemovedFirstDefaultValueInConstructor(b = n).value
fun removedLastDefaultValueInConstructor(n: Int): Int = RemovedLastDefaultValueInConstructor(a = n).value

fun singleVarargArgument(vararg elements: Int): Int = elements.sum()
fun singleVarargArgumentWithDefaultValue(vararg elements: Int = intArrayOf(-1, -2, -3)): Int = elements.sum()
fun varargArgumentAndOtherArguments(first: Int, vararg elements: Int, last: Int): Int = first + elements.sum() + last
fun varargArgumentAndOtherArgumentsWithDefaultValues(first: Int = -100, vararg elements: Int, last: Int = -10): Int = first + elements.sum() + last
fun varargArgumentWithDefaultValueAndOtherArguments(first: Int, vararg elements: Int = intArrayOf(-1, -2, -3), last: Int): Int = first + elements.sum() + last
fun varargArgumentWithDefaultValueAndOtherArgumentsWithDefaultValues(first: Int = -100, vararg elements: Int = intArrayOf(-1, -2, -3), last: Int = -10): Int = first + elements.sum() + last

fun suspendToNonSuspendFunction1(x: Int): Int = runCoroutine { Functions.suspendToNonSuspendFunction(x) }
fun suspendToNonSuspendFunction2(x: Int): Int = runCoroutine { Functions.wrapCoroutine { Functions.suspendToNonSuspendFunction(x) } }
fun suspendToNonSuspendFunction3(x: Int): Int = runCoroutine { runInlined { Functions.suspendToNonSuspendFunction(x) } }
fun nonSuspendToSuspendFunction1(x: Int): Int = Functions.nonSuspendToSuspendFunction(x)
fun nonSuspendToSuspendFunction2(x: Int): Int = runCoroutine { Functions.nonSuspendToSuspendFunction(x) }
fun nonSuspendToSuspendFunction3(x: Int): Int = runInlined { Functions.nonSuspendToSuspendFunction(x) }
fun nonSuspendToSuspendFunction4(x: Int): Int = runCoroutine { runInlined { Functions.nonSuspendToSuspendFunction(x) } }

class InterfaceImpl : Interface {
    override suspend fun suspendToNonSuspendFunction(x: Int): String = Functions.wrapCoroutine { "InterfaceImpl.suspendToNonSuspendFunction($x)" }
    override fun nonSuspendToSuspendFunction(x: Int): String = "InterfaceImpl.nonSuspendToSuspendFunction($x)"
}

class AbstractClassImpl : AbstractClass() {
    override suspend fun suspendToNonSuspendFunction(x: Int): String = Functions.wrapCoroutine { "AbstractClassImpl.suspendToNonSuspendFunction($x)" }
    override fun nonSuspendToSuspendFunction(x: Int): String = "AbstractClassImpl.nonSuspendToSuspendFunction($x)"
}

class OpenClassImpl : OpenClass() {
    override suspend fun suspendToNonSuspendFunction(x: Int): String = Functions.wrapCoroutine { "OpenClassImpl.suspendToNonSuspendFunction($x)" }
    override fun nonSuspendToSuspendFunction(x: Int): String = "OpenClassImpl.nonSuspendToSuspendFunction($x)"

    override suspend fun suspendToNonSuspendFunctionWithDelegation(x: Int): String = super.suspendToNonSuspendFunctionWithDelegation(x) + " called from OpenClassImpl.suspendToNonSuspendFunctionWithDelegation($x)"
    override fun nonSuspendToSuspendFunctionWithDelegation(x: Int): String = super.nonSuspendToSuspendFunctionWithDelegation(x) + " called from OpenClassImpl.nonSuspendToSuspendFunctionWithDelegation($x)"

    override fun openNonInlineToInlineFunction(x: Int): String = "OpenClassImpl.openNonInlineToInlineFunction($x)"
    override fun openNonInlineToInlineFunctionWithDelegation(x: Int): String = super.openNonInlineToInlineFunctionWithDelegation(x) + " called from OpenClassImpl.openNonInlineToInlineFunctionWithDelegation($x)"
    fun newInlineFunction1(x: Int): String = "OpenClassImpl.newInlineFunction1($x)" // overrides accidentally appeared inline function
    @Suppress("NOTHING_TO_INLINE") inline fun newInlineFunction2(x: Int): String = "OpenClassImpl.newInlineFunction2($x)" // overrides accidentally appeared inline function
    @Suppress("NOTHING_TO_INLINE") inline fun newNonInlineFunction(x: Int): String = "OpenClassImpl.newNonInlineFunction($x)" // overrides accidentally appeared non-inline function
}

fun suspendToNonSuspendFunctionInInterface(i: Interface, x: Int): String = runCoroutine { i.suspendToNonSuspendFunction(x) }
fun nonSuspendToSuspendFunctionInInterface(i: Interface, x: Int): String = i.nonSuspendToSuspendFunction(x)
fun suspendToNonSuspendFunctionInInterfaceImpl(ii: InterfaceImpl, x: Int): String = runCoroutine { ii.suspendToNonSuspendFunction(x) }
fun nonSuspendToSuspendFunctionInInterfaceImpl(ii: InterfaceImpl, x: Int): String = ii.nonSuspendToSuspendFunction(x)
fun suspendToNonSuspendFunctionInAbstractClass(ac: AbstractClass, x: Int): String = runCoroutine { ac.suspendToNonSuspendFunction(x) }
fun nonSuspendToSuspendFunctionInAbstractClass(ac: AbstractClass, x: Int): String = ac.nonSuspendToSuspendFunction(x)
fun suspendToNonSuspendFunctionInAbstractClassImpl(aci: AbstractClassImpl, x: Int): String = runCoroutine { aci.suspendToNonSuspendFunction(x) }
fun nonSuspendToSuspendFunctionInAbstractClassImpl(aci: AbstractClassImpl, x: Int): String = aci.nonSuspendToSuspendFunction(x)
fun suspendToNonSuspendFunctionInOpenClass(oc: OpenClass, x: Int): String = runCoroutine { oc.suspendToNonSuspendFunction(x) }
fun nonSuspendToSuspendFunctionInOpenClass(oc: OpenClass, x: Int): String = oc.nonSuspendToSuspendFunction(x)
fun suspendToNonSuspendFunctionInOpenClassImpl(oci: OpenClassImpl, x: Int): String = runCoroutine { oci.suspendToNonSuspendFunction(x) }
fun nonSuspendToSuspendFunctionInOpenClassImpl(oci: OpenClassImpl, x: Int): String = oci.nonSuspendToSuspendFunction(x)
fun suspendToNonSuspendFunctionWithDelegation(oci: OpenClassImpl, x: Int): String = runCoroutine { oci.suspendToNonSuspendFunctionWithDelegation(x) }
fun nonSuspendToSuspendFunctionWithDelegation(oci: OpenClassImpl, x: Int): String = oci.nonSuspendToSuspendFunctionWithDelegation(x)

fun openNonInlineToInlineFunctionInOpenClass(oc: OpenClass, x: Int): String = oc.openNonInlineToInlineFunction(x)
fun openNonInlineToInlineFunctionWithDelegationInOpenClass(oc: OpenClass, x: Int): String = oc.openNonInlineToInlineFunctionWithDelegation(x)
fun newInlineFunction1InOpenClass(oc: OpenClass, x: Int): String = oc.newInlineFunction1Caller(x)
fun newInlineFunction2InOpenClass(oc: OpenClass, x: Int): String = oc.newInlineFunction2Caller(x)
fun newNonInlineFunctionInOpenClass(oc: OpenClass, x: Int): String = oc.newNonInlineFunctionCaller(x)
fun openNonInlineToInlineFunctionInOpenClassImpl(oci: OpenClassImpl, x: Int): String = oci.openNonInlineToInlineFunction(x)
fun openNonInlineToInlineFunctionWithDelegationInOpenClassImpl(oci: OpenClassImpl, x: Int): String = oci.openNonInlineToInlineFunctionWithDelegation(x)
fun newInlineFunction1InOpenClassImpl(oci: OpenClassImpl, x: Int): String = oci.newInlineFunction1(x)
fun newInlineFunction2InOpenClassImpl(oci: OpenClassImpl, x: Int): String = oci.newInlineFunction2(x)
fun newNonInlineFunctionInOpenClassImpl(oci: OpenClassImpl, x: Int): String = oci.newNonInlineFunction(x)

fun inlineLambdaToNoinlineLambda(x: Int): String = Functions.inlineLambdaToNoinlineLambda(x) { if (it > 0) it.toString() else return "inlineLambdaToNoinlineLambda($x)" }
fun inlineLambdaToCrossinlineLambda(x: Int): String = Functions.inlineLambdaToCrossinlineLambda(x) { if (it > 0) it.toString() else return "inlineLambdaToCrossinlineLambda($x)" }

fun nonLocalReturnFromArrayConstructorLambda(expected: String, unexpected: String): String = Array(1) outer@{
    Array(1) {
        if ('1' in "123") { // The condition that is always true.
            return@outer expected
        }
        unexpected
    }[0]
}[0]

fun nonLocalReturnFromIntArrayConstructorLambda(expected: Int, unexpected: Int): Int = IntArray(1) outer@{
    IntArray(1) {
        if ('1' in "123") { // The condition that is always true.
            return@outer expected
        }
        unexpected
    }[0]
}[0]
