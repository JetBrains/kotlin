// SKIP_TXT
// FIR_IDENTICAL

interface Some {
    fun foo(b: Boolean? = null): Int = 10
}

class SomeImpl : Some {
    override fun foo(b: Boolean?): Int {
        return 0
    }

    private fun buz() {
        bar(::foo)
    }
}

private fun buz() {
    bar(SomeImpl()::foo)
}

private fun <T> bar(actionForAll: () -> T) {
    actionForAll()
}
