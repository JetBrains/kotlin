import kotlin.wasm.WasmImport

@WasmImport("a", "b")
external fun foo0(): Unit

@WasmImport("a", "b")
fun foo1() {
}

external class C {
    @WasmImport("a", "b")
    fun memberFunction()
}

fun foo2() {
    @WasmImport("a", "b")
    fun localFun() {
    }
}

val p1 = (@WasmImport("a", "b") fun () {})

@WasmImport("a", "b")
external fun foo3(
    p0: Unit,
    p1: String,
    p2: Any,
    p3: Int?,
    p4: Boolean?
): Unit


@WasmImport("a", "b")
external fun returnNullableUnit(): Unit?

@WasmImport("a", "b")
external fun returnNullableBoolean(): Boolean?

@WasmImport("a", "b")
external fun returnNullableAny(): Any?

@WasmImport("a", "b")
external fun <T> fooGeneric(x: T): T

@WasmImport("a", "b")
external fun fooDeafultAndVararg(
a: Int = definedExternally,
vararg b: Int
): Unit
