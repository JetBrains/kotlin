fun foo() {
    outer@while (true) {
        try {
            while (true) {
                continue@outer
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
                continue@outer
            }
        } finally {
            return "OK"
        }
    }
}
