class Foo<K>

fun <K> buildFoo(builderAction: Foo<K>.() -> Unit): Foo<K> = Foo()

class Bar<K>

fun <K: Bar<N>, N: Bar<K>> Foo<K>.bar(x: Int = 1) {}

fun main() {
    val x = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>buildFoo<!> {
        bar()
    }
}
