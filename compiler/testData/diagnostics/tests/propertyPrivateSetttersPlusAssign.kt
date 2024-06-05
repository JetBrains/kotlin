// ISSUE: KT-68521

class Test {
    var x = 10
        private set
}

fun main() {
    val test = Test()

    <!INVISIBLE_SETTER!>test.x<!> = 5
    <!INVISIBLE_SETTER!>test.x<!> -= 5
    <!INVISIBLE_SETTER!>test.x<!>--
    --<!INVISIBLE_SETTER!>test.x<!>
}
