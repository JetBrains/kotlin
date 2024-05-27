// ISSUE: KT-68531
enum class Some {
    A {
        init {
            A = null!!
            B = null!!
        }
    },
    B {
        init {
            A = null!!
            B = null!!
        }
    };

    init {
        A = null!!
        B = null!!
    }
}

fun test() {
    Some.A = null!!
    Some.B = null!!
}
