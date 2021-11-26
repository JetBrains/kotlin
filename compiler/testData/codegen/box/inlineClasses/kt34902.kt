// WITH_STDLIB

interface OneofField<T> {
    val value: T
    val number: Int
    val name: String

    data class OneofUint32 constructor(
        override val value: UInt,
        override val number: Int = 111,
        override val name: String = "oneof_uint32"
    ) : OneofField<UInt>
}

fun box(): String {
    val d = OneofField.OneofUint32(0u)
    val s = d.toString()
    if (s != "OneofUint32(value=0, number=111, name=oneof_uint32)") return s
    return "OK"
}