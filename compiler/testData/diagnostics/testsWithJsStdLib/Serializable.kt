fun use(<!UNUSED_PARAMETER!>s<!>: java.io.Serializable) {

}

fun useList(<!UNUSED_PARAMETER!>s<!>: List<java.io.Serializable>) {

}

fun testPrimitives(b: Byte, ss: Short, i: Int, l: Long, d: Double, s: String, f: Float, bool: Boolean) {
    use(<!TYPE_MISMATCH!>b<!>)
    use(<!TYPE_MISMATCH!>ss<!>)
    use(<!TYPE_MISMATCH!>i<!>)
    use(<!TYPE_MISMATCH!>l<!>)
    use(<!TYPE_MISMATCH!>s<!>)
    use(<!TYPE_MISMATCH!>f<!>)
    use(<!TYPE_MISMATCH!>d<!>)
    use(<!TYPE_MISMATCH!>bool<!>)
}

class N
class S: java.io.Serializable

fun testArrays(ia: IntArray, ai: Array<Int>, an: Array<N>, a: Array<S>) {
    use(<!TYPE_MISMATCH!>ia<!>)
    use(<!TYPE_MISMATCH!>ai<!>)
    use(<!TYPE_MISMATCH!>an<!>)
    use(<!TYPE_MISMATCH!>a<!>)
}

fun testLiterals() {
    use(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    use(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1.0<!>)
    use(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>11111111111111<!>)
    use(<!TYPE_MISMATCH!>"Asdsd"<!>)
    use(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>true<!>)
}

fun testNotSerializable(l: List<Int>) {
    use(<!TYPE_MISMATCH!>l<!>)
    use(<!TYPE_MISMATCH!>N()<!>)
}

enum class C {
    E, E2
}

fun testEnums(a: Enum<*>) {
    use(<!TYPE_MISMATCH!>C.E<!>)
    use(<!TYPE_MISMATCH!>C.E2<!>)
    use(<!TYPE_MISMATCH!>a<!>)
}

fun testLists(a: List<Int>) {
    useList(<!TYPE_MISMATCH!>a<!>)
}