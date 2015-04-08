public trait Foo {
    override fun <lineMarker descr="Overrides function in 'Any'"></lineMarker>toString() = "str"
}

/*
LINEMARKER: Overrides function in 'Any'
TARGETS:
Any.kt
    public open fun <1>toString(): String
*/