// K2: See KT-65342

fun test() {
    <!MULTIPLE_LABELS_ARE_FORBIDDEN!>a@<!> b@ while(true) {
        val f = {
            return@a
        }
        break@b
    }
}
