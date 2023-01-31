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
tailrec fun tailrecToNonTailrec(n: Int): Int = Functions.tailrecToNonTailrec(n, 1)

fun removedDefaultValueInFunction(n: Int): Int = Functions.removedDefaultValue(n)
fun removedDefaultValueInConstructor(n: Int): Int = RemovedDefaultValueInConstructor(n).value

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
