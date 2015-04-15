fun box(): String {
    try {
        test {
            return@box "OK"
        }
    } finally {
    }

    return "fail"
}