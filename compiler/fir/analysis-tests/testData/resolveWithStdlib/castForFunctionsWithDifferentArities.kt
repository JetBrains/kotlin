// ISSUE: KT-63865

fun test(b: Boolean, block1: Any.() -> Unit, block2: (Any.(Any?) -> Unit)?) {
    if (b) {
        requireNotNull(block1)
    } else {
        requireNotNull(block2)
    }
}
