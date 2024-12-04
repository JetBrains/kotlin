// RUN_PIPELINE_TILL: FRONTEND
// K2: See KT-65342

fun test() {
    <!MULTIPLE_LABELS_ARE_FORBIDDEN!>a@<!> b@ while(true) {
        val f = {
            <!NOT_A_FUNCTION_LABEL!>return@a<!>
        }
        break@b
    }
}
