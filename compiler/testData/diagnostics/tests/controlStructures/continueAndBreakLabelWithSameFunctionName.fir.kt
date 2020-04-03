fun test1() {
    test1@ for(i in 1..2) {
        continue@test1
    }
}

fun test2() {
    test2@ while (true) {
        break@test2
    }
}

class Test3 {
    fun Test3() {
        Test3@ while (true) {
            break@Test3
        }
    }
}

fun test4() {
    <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break@test4<!>
}

class Test5 {
    fun Test5<!SYNTAX!><!> {
        return@Test5
    }
}

class Test6 {
    fun Test6() {
        Test6@ while (true) {
            break@Test6
        }

        Test6@ while (true) {
            break@Test6
        }
    }
}

class Test7 {
    fun Test7() {
        Test8@ while (true) {
            <!NOT_A_LOOP_LABEL!>break@Test7<!>
        }

        Test7@ while (true) {
            <!NOT_A_LOOP_LABEL!>break@Test8<!>
        }
    }
}