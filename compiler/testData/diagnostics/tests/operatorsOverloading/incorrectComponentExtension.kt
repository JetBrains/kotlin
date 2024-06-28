// FIR_IDENTICAL
class DummyClass001

fun DummyClass001.component1() = "1"
fun DummyClass001.component2() = "2"

fun testDummyClass001() {
    val (<!OPERATOR_MODIFIER_REQUIRED!>a<!>, <!OPERATOR_MODIFIER_REQUIRED!>b<!>) = DummyClass001()
}