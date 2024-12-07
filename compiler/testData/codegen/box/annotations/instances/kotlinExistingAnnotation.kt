// TARGET_BACKEND: JVM_IR
// JVM_ABI_K1_K2_DIFF: K1 and K2 store annotation properties in the different order
// WITH_STDLIB
// LANGUAGE: +InstantiationOfAnnotationClasses

fun f(): Metadata = Metadata(
    kind = 0,
    metadataVersion = intArrayOf(),
    data1 = arrayOf(),
    data2 = arrayOf(),
    extraString = "",
    packageName = "foo",
    extraInt = 0,
    bytecodeVersion = intArrayOf(1, 0, 3),
)

fun box(): String {
    val m = f()
    if (m.toString() == """@kotlin.Metadata(kind=0, metadataVersion=[], bytecodeVersion=[1, 0, 3], data1=[], data2=[], extraString=, packageName=foo, extraInt=0)""" ||  // K2
        m.toString() == """@kotlin.Metadata(bytecodeVersion=[1, 0, 3], data1=[], data2=[], extraInt=0, extraString=, kind=0, metadataVersion=[], packageName=foo)""")    // K1
        return "OK"
    return m.toString()
}
