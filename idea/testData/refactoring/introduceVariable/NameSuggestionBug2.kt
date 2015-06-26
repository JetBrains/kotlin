class For {
    var xxx: For? = null
}


fun foo(f: For) {
    <selection>f.xxx</selection>

    if (true) {
        val xxx = 1
    }
}