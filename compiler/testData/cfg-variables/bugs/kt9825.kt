fun test5() {
    var a: Int
    try {
        a = 3
    }
    finally {
        a = 5
    }
    a.hashCode()
}