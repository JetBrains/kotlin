// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57754, KT-57436

interface I {
    val <T> T.id: T
        get() = this
}

class A(i: I) : I by i
