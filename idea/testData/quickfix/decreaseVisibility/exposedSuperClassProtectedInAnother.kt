// "Make 'First' private" "true"
// ACTION: Make 'Data' public

class Other {
    internal open class Data(val x: Int)
}

class Another {
    protected class First : Other.<caret>Data(42)
}