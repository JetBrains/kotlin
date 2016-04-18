// "Make First private" "true"

class Other {
    internal open class Data(val x: Int)
}

class Another {
    protected class First : Other.<caret>Data(42)
}