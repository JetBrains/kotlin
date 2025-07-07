// ISSUE: KT-78960
// LANGUAGE: +IrInlinerBeforeKlibSerialization
// IGNORE_BACKEND: WASM
// ^^^ java.lang.NullPointerException: null
//	at org.jetbrains.kotlin.backend.wasm.lower.EraseVirtualDispatchReceiverParametersTypes.lower
//     After the test will be fixed, please delete this test,
//     and remove `LANGUAGE: -IrInlinerBeforeKlibSerialization` from test accessorForFakeOverride.kt

internal open class A(val value: String) {
    private val getValuePrivate get() = value
    inline fun getValuePublic() = getValuePrivate
}

internal class B(value: String): A(value)

fun box(): String {
    return B("OK").getValuePublic()
}
