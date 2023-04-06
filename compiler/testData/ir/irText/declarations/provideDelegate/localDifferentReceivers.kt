// FIR_IDENTICAL
// WITH_STDLIB
// DUMP_LOCAL_DECLARATION_SIGNATURES

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57434

class MyClass(val value: String)

operator fun MyClass.provideDelegate(host: Any?, p: Any): String =
        this.value

operator fun String.getValue(receiver: Any?, p: Any): String =
        this


fun box(): String {
    val testO by MyClass("O")
    val testK by "K"
    val testOK = testO + testK

    return testOK
}
