fun box() : String {
    try {
        removeInIterator.bar(object : Iterator<Int> {
            public override fun hasNext(): Boolean = false
            public override fun next(): Int = 1
        })
    }
    catch (e: UnsupportedOperationException) {
        if (e.getMessage() == "Mutating method called on a Kotlin Iterator")
            return "OK"
    }
    return "fail"
}
