//KT-1820 Write test for ASSIGN_OPERATOR_AMBIGUITY
package kt1820

class MyInt(val i: Int) {
    fun plus(m: MyInt) : MyInt = MyInt(m.i + i)
}

fun Any.plusAssign(<!UNUSED_PARAMETER!>a<!>: Any) {}

fun test(m: MyInt) {
    m <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> m

    var i = 1
    i <!ASSIGN_OPERATOR_AMBIGUITY!>+=<!> 34
}


