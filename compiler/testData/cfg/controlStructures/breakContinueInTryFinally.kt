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
    println("OK")
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
