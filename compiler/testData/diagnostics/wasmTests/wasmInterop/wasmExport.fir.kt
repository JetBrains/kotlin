import kotlin.wasm.WasmExport

@WasmExport("a")
external fun foo0(): Unit

@WasmExport("a")
fun foo1(): Int = js("42")

class C() {
    @WasmExport("a")
    fun foo2(): Int = 42
}

@WasmExport("a")
fun foo3(): Int = 42

@WasmExport()
fun foo4(): Int = 42

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport()
@WasmExport()
fun foo6(): Int = 42

val p1 = (@WasmExport("a") fun () {})

@WasmExport("a")
fun foo7(
p0: Unit,
p1: String,
p2: Any,
p3: Int?,
p4: Boolean?
): Unit {
    p0.toString()
    p1.toString()
    p2.toString()
    p3.toString()
    p4.toString()
}

@WasmExport("a")
fun returnNullableUnit(): Unit? { return null }

@WasmExport("a")
fun returnNullableBoolean(): Boolean? { return null }

@WasmExport("a")
fun returnNullableAny(): Any?  { return null }

@WasmExport("a")
fun <T> fooGeneric(x: T): T { return x }

@WasmExport("a")
fun fooDeafultAndVararg(
a: Int = definedExternally,
vararg b: Int
): Unit { b.toString() }
