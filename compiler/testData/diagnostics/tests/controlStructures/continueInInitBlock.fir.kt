// ISSUE: KT-47892

fun test(b: Boolean)  {
    while (b) {
        class A {
            init {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }
            constructor(): super()
        }
    }
}
