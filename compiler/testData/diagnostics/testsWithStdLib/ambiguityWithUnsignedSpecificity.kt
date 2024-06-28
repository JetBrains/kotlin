// FIR_IDENTICAL
// ISSUE: KT-57568

fun <T, K> T.convert(): K = null!!

fun of(size: ULong) {
    of(size.convert())
}

fun of(size: Int) {}

fun of(size: Long) {}
