// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER -USELESS_CAST -UNCHECKED_CAST -USELESS_IS_CHECK

object Obj : suspend () -> Unit {
    override suspend fun invoke() {}
}

class Cls : suspend () -> Unit {
    override suspend fun invoke() {}
}

interface SubInt : suspend () -> Unit

class SubCls : SubInt {
    override suspend fun invoke() {}
}

fun takeSuspend(f: suspend () -> Unit) {}
fun takeGeneric(f: Any) {}

suspend fun foo() {}
fun bar() {}

fun testArguments() {
    takeSuspend(<!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Obj<!>)
    takeSuspend(<!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Cls()<!>)
    takeSuspend(<!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>SubCls()<!>)

    // Positive: no explicit type on the val, so its inferred type is the real object's type - safely unwrapped to the constructor call
    val o = Obj
    takeSuspend(<!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>o<!>)

    val c = Cls()
    takeSuspend(<!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>c<!>)

    // Positive: chained implicitly-typed vals still unwrap transitively, each hop has exactly one initializer
    val c2 = c
    takeSuspend(<!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>c2<!>)

    // Negative: `var` can be reassigned, so its single initializer can't be trusted without data-flow
    var vc = Cls()
    takeSuspend(vc)

    // Negative: explicit type on the val hides the initializer's real shape, can't unwrap without data-flow
    val sc: SubInt = <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>SubCls()<!>
    takeSuspend(sc)

    // Safe / Negative cases
    takeSuspend { }
    takeSuspend(::foo)
    takeSuspend(::bar)

    val nonSuspendLambda = {}
    takeSuspend(nonSuspendLambda)

    val suspendLambda: suspend () -> Unit = {}
    takeSuspend(suspendLambda)

    // Negative: parameter's declared type isn't suspend-functional (just `Any`)
    takeGeneric(Obj)
    takeGeneric(Cls())
}

fun <T> takeGenericParam(value: T) {}
fun <K, V> putEntry(map: MutableMap<K, V>, key: K, value: V) {}

fun testGenericSubstitution() {
    // Negative: type parameter only *substituted* to a suspend-functional type, not declared as one
    takeGenericParam<suspend () -> Unit>(Obj)
    takeGenericParam<suspend () -> Unit>(Cls())

    val map = mutableMapOf<suspend () -> Unit, String>()
    putEntry(map, Obj, "obj")
    putEntry(map, Cls(), "cls")
}

fun testCasts() {
    <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Obj as (suspend () -> Unit)<!>
    <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Obj as? (suspend () -> Unit)<!>
    <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Cls() as (suspend () -> Unit)<!>
    <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Cls() as? (suspend () -> Unit)<!>
    <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Any() as (suspend () -> Unit)<!>
    <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Any() as? (suspend () -> Unit)<!>

    // Safe / Negative cases
    val suspendLambda: suspend () -> Unit = {}
    suspendLambda as (suspend () -> Unit)
    suspendLambda as? (suspend () -> Unit)

    ({ }) as (suspend () -> Unit)
    (::foo) as (suspend () -> Unit)

    // Positive: no explicit type on the val, so its inferred type is the real object's type - safely unwrapped to the constructor call
    val c = Cls()
    <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>c as (suspend () -> Unit)<!>
    <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>c as? (suspend () -> Unit)<!>

    // is-checks should remain untouched
    if (Obj is (suspend () -> Unit)) {}
    if (Obj !is (suspend () -> Unit)) {}
}

fun testSuppression() {
    @Suppress("JS_SUSPEND_FUNCTION_INTERFACE_CAST")
    val suppressed: suspend () -> Unit = Cls()
}
