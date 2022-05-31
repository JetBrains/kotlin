// !RENDER_DIAGNOSTICS_FULL_TEXT
class Foo<K>

fun <K> buildFoo(builderAction: Foo<K>.() -> Unit): Foo<K> = Foo()

fun <K> Foo<K>.bar(x: Int = 1) {}

fun main() {
    val x = <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>buildFoo<!> {
        bar()
    }
}
