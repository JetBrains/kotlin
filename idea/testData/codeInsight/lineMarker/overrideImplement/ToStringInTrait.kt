public interface Foo {
    override fun <lineMarker descr="Overrides function in 'Any'"></lineMarker>toString() = "str"
}

/*
LINEMARKER: Overrides function in 'Any'
TARGETS:
Any.kotlin_class
    public open fun <1>toString(): kotlin.String { /* compiled code */ }
*/