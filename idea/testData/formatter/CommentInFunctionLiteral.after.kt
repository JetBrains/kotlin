fun test(some: (Int) -> Int) {
}

fun foo() {
    test() {
        // Some comment
        it
    }

    test() {
//        val a = 42
    }

    test() {
/*
        val a = 42
*/
    }

    test() {
        /*
            val a = 42
        */
    }

    test() {
//        val a = 42
        val b = 44
    }
}

val s = Shadow { -> // wdwd
    val a = 42
}

val s2 = Shadow { // wdwd
    val a = 42
}

val s3 = Shadow(
        fun() {  // wdwd
            val a = 42
        },
)

val s4 = Shadow(
        fun() { /* s */
            val a = 42
        },
)

val s5 = Shadow { ->
    // wdwd
    val a = 42
}

val s6 = Shadow {
    // wdwd
    val a = 42
}

val s7 = Shadow(
        fun() {
            // wdwd
            val a = 42
        },
)

val s8 = Shadow(
        fun() {
            // wdwd
            val a = 42
        },
)

val s9 = Shadow { ->   /* s */
    val a = 42
}

class Shadow(callback: () -> Unit)
