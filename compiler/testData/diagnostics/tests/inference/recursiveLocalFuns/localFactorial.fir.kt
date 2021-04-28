// FIR_IDE_IGNORE
// !WITH_NEW_INFERENCE
// See KT-6271
fun foo() {
    fun fact(n: Int) = {
        if (n > 0) {
            fact(n - 1) <!UNRESOLVED_REFERENCE!>*<!> n
        }
        else {
            1
        }
    }
}
