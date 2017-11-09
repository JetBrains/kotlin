class C {
    var s<caret>: Int

    init {
        s = 1
        s.hashCode()
        s = 2
    }
}