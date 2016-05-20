fun sample(): String? {
    try {
        if (false) {
            return "fail"
        } else {
            if (false) {
                if (false) {
                    var foo: String? = null
                    try {
                        foo = "test"
                    } catch (e: Exception) {
                        return "fail"
                    } finally {
                        println(foo) // Variable 'foo' must be initialized
                    }
                }
                return "fail"
            }
        }
    } finally {}
    return null
}
