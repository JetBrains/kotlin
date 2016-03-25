fun foo() {
    outer@while (true) {
        try {
            while (true) {
                <!UNREACHABLE_CODE!>continue@outer<!>
            }
        } finally {
            break
        }
    }
    "OK".hashCode()
}

fun bar(): String {
    outer@while (true) {
        try {
            while (true) {
                <!UNREACHABLE_CODE!>continue@outer<!>
            }
        } finally {
            return "OK"
        }
    }
}
