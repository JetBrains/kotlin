// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

// Generic interface, nothing interesting here
interface UseCase<in I, out O> {
    fun execute(input: I): O
}

// We don't want to call use cases without input with explicit Unit argument, so we create this handy extension
fun <O> UseCase<Unit, O>.execute(): O = execute(Unit)

class Foo : UseCase<Unit, List<List<List<String>>>> {
    override fun execute(input: Unit) = listOf(listOf(listOf("foo")))
}

class Bar : UseCase<Unit, List<List<String>>> {
    override fun execute(input: Unit) = listOf(listOf("bar"))
}

class Baz : UseCase<Unit, List<List<List<List<List<List<String>>>>>>> {
    override fun execute(input: Unit) = listOf(listOf(listOf(listOf(listOf(listOf("foo"))))))
}

fun main() {
    val foo = Foo()
    foo.execute(Unit) // Member: OK both versions
    foo.execute() // Extension: OI - OK, NI - was error: "Not enough information to infer type variable O"

    val bar = Bar()
    bar.execute(Unit) // Member: OK both versions
    bar.execute() // Extension: OK both versions

    val baz = Baz()
    baz.execute(Unit)
    baz.execute()
}
