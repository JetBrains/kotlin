// WITH_STDLIB

interface Result

interface Foo {
    suspend operator fun Result.component1(): Any = TODO()
}

fun use(c: suspend Foo.() -> Unit) {}

fun generate(): Result = TODO()

fun test() {
    use {
        // This component1 accesses both dispatch and extension receivers,
        // So, there should be p$
        val (value) = generate()
    }
}