fun box(): String {
    OUTER@while (true) {
        var x = ""
        try {
            do {
                x = x + break@OUTER
            } while (true)
        } finally {
            return "OK"
        }
    }
}