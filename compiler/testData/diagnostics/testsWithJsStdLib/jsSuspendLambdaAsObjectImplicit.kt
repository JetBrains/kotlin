// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FunctionalTypeWithExtensionAsSupertype
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

object Obj : suspend () -> Unit {
    override suspend fun invoke() {}
}

class Cls : suspend () -> Unit {
    override suspend fun invoke() {}
}

class RCls : suspend Int.() -> Unit {
    override suspend fun invoke(p1: Int) {}
}

// Properties / local variables with an explicit suspend-functional type

val topLevelProperty: suspend () -> Unit = <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Cls()<!>

class Container {
    val memberProperty: suspend () -> Unit = <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Cls()<!>
    val memberPropertyWithReceiver: suspend Int.() -> Unit = <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>RCls()<!>

    // Negative: no explicit type -> inferred type is the class itself, not a boundary crossing
    val memberPropertyInferred = Cls()
}

fun testLocalProperties() {
    val local: suspend () -> Unit = <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Cls()<!>
    val localWithReceiver: suspend Int.() -> Unit = <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>RCls()<!>

    // Negative: no explicit type
    val localInferred = Cls()

    // Negative: real lambda / callable reference
    val localLambda: suspend () -> Unit = { }
}

// Default parameter values

fun withDefault(f: suspend () -> Unit = <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Cls()<!>) {}
fun withDefaultReceiver(f: suspend Int.() -> Unit = <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>RCls()<!>) {}

// Negative: real lambda as default
fun withSafeDefault(f: suspend () -> Unit = { }) {}

// Return statements

fun makeExplicit(): suspend () -> Unit {
    return <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Cls()<!>
}

fun makeImplicit(): suspend () -> Unit = <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>Cls()<!>

fun makeWithReceiver(): suspend Int.() -> Unit {
    return <!JS_SUSPEND_FUNCTION_INTERFACE_CAST!>RCls()<!>
}

// Negative: return type is inferred, no declared contract crossed
fun makeInferred() = Cls()

// Negative: real lambda returned
fun makeSafe(): suspend () -> Unit {
    return { }
}
