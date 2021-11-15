// TARGET_BACKEND: JVM_IR

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

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
    if (m.toString() == """@kotlin.Metadata(bytecodeVersion=[1, 0, 3], data1=[], data2=[], extraInt=0, extraString=, kind=0, metadataVersion=[], packageName=foo)""")
        return "OK"
    return m.toString()
}
