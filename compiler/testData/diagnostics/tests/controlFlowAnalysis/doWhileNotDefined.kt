// FIR_IDENTICAL
// SKIP_TXT
// ISSUE: KT-64872, KT-65911

fun test1(cond1: Boolean) {
    do {
        if (cond1) continue
        val cond2 = false
    } while (<!UNINITIALIZED_VARIABLE!>cond2<!>) // cond2 may be not defined here
}

fun test2(cond1: Boolean, cond3: Boolean) {
    do {
        if (cond1) continue
        val cond2 = false
    } while (
        run {
            do {
                if (cond3) continue
                val cond4 = false
            } while (<!UNINITIALIZED_VARIABLE!>cond4<!>) // cond4 may be not defined here

            <!UNINITIALIZED_VARIABLE!>cond2<!> // cond2 may be not defined here
        }
    )
}

fun test3(cond1: Boolean, cond3: Boolean) {
    do {
        if (cond1) continue
        val cond2 = false
    } while (
        run {
            do {
                if (cond3) continue
                val cond4 = false
            } while (<!UNINITIALIZED_VARIABLE!>cond2<!> && <!UNINITIALIZED_VARIABLE!>cond4<!>) // cond2 and cond4 may be not defined here

            cond3
        }
    )
}

fun test4() {
    try {
        for (i in 0..100) {
            var counter = 0
            do {
                try {
                } finally {
                    counter++
                }
            } while (counter < 500)
        }
    } catch (e: Exception) {
        e.cause?.let {}
        e.let {}
    }
}
