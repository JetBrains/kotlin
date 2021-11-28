package test

fun use() {
    Default.create()

    Explicit.<!INVISIBLE_REFERENCE!>create<!>()
}

private class Default {
    companion object {
        fun create() = Default()
    }
}

private class Explicit {
    private companion object {
        fun create() = Explicit()
    }
}
