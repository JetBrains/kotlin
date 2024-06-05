// ISSUE: KT-68521

class Test {
    var x = 10
        private set
}

fun main() {
    val test = Test()

    test.<!INVISIBLE_SETTER!>x<!> = 5
    test.x -= 5
    test.x--
    --test.x
}
