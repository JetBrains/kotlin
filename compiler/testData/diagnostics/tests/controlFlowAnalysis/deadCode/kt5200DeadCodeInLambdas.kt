//KT-5200 Mark unreachable code in lambdas

fun test1(): String {
    doCall local@ {
        throw NullPointerException()
        <!UNREACHABLE_CODE!>"b3"<!> //unmarked
    }

    return "OK"
}

fun test2(nonLocal: String, b: Boolean): String {
    doCall local@ {
        if (b) {
            return@local "b1"
        } else {
            return@local "b2"
        }
        <!UNREACHABLE_CODE!>"b3"<!> //unmarked
    }

    return nonLocal
}

inline fun doCall(block: ()-> String) = block()
