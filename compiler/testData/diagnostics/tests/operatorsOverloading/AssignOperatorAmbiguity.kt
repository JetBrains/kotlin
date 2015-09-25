//KT-1820 Write test for ASSIGN_OPERATOR_AMBIGUITY
package kt1820

class MyInt(val i: Int) {
    operator fun plus(m: MyInt) : MyInt = MyInt(m.i + i)
}

operator fun Any.plusAssign(<!UNUSED_PARAMETER!>a<!>: Any) {}

fun test(m: MyInt) {
    m += m

    var i = 1
    i <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> 34
}


