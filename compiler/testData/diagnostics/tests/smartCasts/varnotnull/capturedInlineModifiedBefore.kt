// WITH_STDLIB
// ISSUE: KT-46586

inline fun run(block: () -> Unit) = block()

fun test() {
    var label: String? = null
    run {
        label = "zzz"
    }
    if (label == null) {
        label = "zzz"
    }
    <!SMARTCAST_IMPOSSIBLE!>label<!>.isBlank()
}
