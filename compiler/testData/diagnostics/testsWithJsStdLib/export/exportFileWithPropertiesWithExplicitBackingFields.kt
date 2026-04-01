// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ExplicitBackingFields
@file:JsExport

interface I

@JsExport.Ignore
value class V(val x: Int) : I  {
    constructor() : this(42) {}
}

val p0: Any
    field = 42

val p1: Any
    field = V(42)

val p2: I
    field = V(42)

val p3: I
    field = V()

class A {
    val p4: I field: V

    init {
        p4 = V(42)
    }
}