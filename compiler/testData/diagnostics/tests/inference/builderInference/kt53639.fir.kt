// WITH_STDLIB
// SKIP_TXT
fun <From, To> InputWrapper<From>.doMapping(
    foo: (From) -> List<To>,
    bar: (List<To>) -> Boolean = { it.isNotEmpty() },
) = InputWrapper(value = foo(value))

data class InputWrapper<TItem>(val value: TItem)

data class Output(val source: InputWrapper<List<String>>)

fun main2(input: InputWrapper<Unit>): Output {
    val output = input.doMapping(
        foo = { buildList { add("this is List<String>") } },
        bar = { it.isNotEmpty() },
    )

    return Output(source = output)
}
