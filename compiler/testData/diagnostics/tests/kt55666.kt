// FIR_IDENTICAL

inline fun l2f1(p: () -> Unit) {}

fun label2simple1() {
    l2f1 { return@label2simple1 }

    fun local() {
        l2f1 { return@local }
    }

    labelLocal@ fun labelledLocal() {
        l2f1 { return@labelLocal }
    }
}

fun main() {
    label2simple1()
}
