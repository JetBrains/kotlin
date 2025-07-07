// ISSUE: KT-78960
// LANGUAGE: -IrInlinerBeforeKlibSerialization
// ^^^ After accessorForFakeOverrideWithInlinedFunInKlib.kt will be fixed, please delete that test,
//     and remove `LANGUAGE: -IrInlinerBeforeKlibSerialization` from this test

internal open class A(val value: String) {
    private val getValuePrivate get() = value
    inline fun getValuePublic() = getValuePrivate
}

internal class B(value: String): A(value)

fun box(): String {
    return B("OK").getValuePublic()
}
