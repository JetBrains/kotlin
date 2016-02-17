public interface Foo {
    override fun <lineMarker descr="Overrides function in 'Any'"></lineMarker>toString() = "str"
}

/*
LINEMARKER: Overrides function in 'Any'
TARGETS:
kotlin.kotlin_builtins
    public open fun <1>toString(): kotlin.String { /* compiled code */ }
*/
