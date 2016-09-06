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

fun baz(): String {
    outer@while (true) {
        try {
            inner@while (true) {
                continue@inner
            }
        } finally {
            return "OK"
        }
    }
}
