fun foo(x: Any) {
    if (x is String) {
        System.getProperty("abc".substring(<selection>1</selection>))
        println()
    }
    else {
        println()
    }
}
