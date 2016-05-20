// "Make 'Data' public" "true"
// ACTION: Make 'First' private

class Other {
    internal open class Data(val x: Int)
}

class Another {
    protected class First : Other.<caret>Data(42)
}