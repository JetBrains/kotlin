fun test() {
    while (true) {
        fun local1() {
            <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
        }
    }
}

fun test2() {
    while (true) {
        <!UNUSED_LAMBDA_EXPRESSION!>{
            <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
        }<!>
    }
}

fun test3() {
    while (true) {
        class LocalClass {
            init {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }

            fun foo() {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
            }
        }
    }
}

fun test4() {
    while (true) {
        object: Any() {
            init {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
            }
        }
    }
}

fun test5() {
    while (true) {
        class LocalClass(val x: Int) {
            constructor() : this(42) {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
            }
            constructor(y: Double) : this(y.toInt()) {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }
        }
    }
}

fun test6() {
    while (true) {
        class LocalClass(val x: Int) {
            init {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
            }
            init {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }
        }
    }
}

fun test7() {
    while (true) {
        class LocalClass {
            val x: Int = if (true) {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
            }
            else {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }
        }
    }
}

fun test8() {
    while (true) {
        class LocalClass(val x: Int) {
            constructor() : this(if (true) { 42 } else { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> })
        }
    }
}