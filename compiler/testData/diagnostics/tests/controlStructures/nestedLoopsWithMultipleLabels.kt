// K2: See KT-65342

fun test() {
    a@ b@ while(true) {
        val f = {
            <!NOT_A_FUNCTION_LABEL!>return@a<!>
        }
        break@b
    }
}
