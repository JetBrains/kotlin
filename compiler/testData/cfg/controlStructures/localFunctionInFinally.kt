fun foo(): Int {
    var i = 0
    try {
        i = 1
    }
    finally {
        fun bar() {}
        return i
    }
}