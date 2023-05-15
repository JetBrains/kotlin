// ISSUE: KT-44554
// FIR_DUMP

data class Foo(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>a: Int<!>, val b: Int) {
    val c = 4
}
