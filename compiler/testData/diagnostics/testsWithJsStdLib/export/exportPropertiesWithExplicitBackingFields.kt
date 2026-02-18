// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ExplicitBackingFields
@JsExport
interface I

value class V(val x: Int) : I {
    constructor() : this(42) {}
}

@JsExport
val p0: Any
    field = 42

@JsExport
val p1: Any
    field = V(42)

@JsExport
val p2: I
    field = V(42)

@JsExport
val p3: I
    field = V()

@JsExport
class A {
    val p4: I field: V

    init {
        p4 = V(42)
    }
}
