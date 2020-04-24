fun box(): String {
    var result = ""
    run {
        object {
            init {
                result = "OK"
            }
        }
    }
    return result
}

// 1 LINENUMBER 6