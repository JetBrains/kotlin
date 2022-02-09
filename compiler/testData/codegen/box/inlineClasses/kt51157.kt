// WITH_STDLIB

interface ByteArrayParser<Output> : GenericParser<ByteArray, Output> {
    override fun repExactly(n: UInt): ByteArrayParser<List<Output>> =
        super.repExactly(n) as ByteArrayParser<List<Output>>
}

interface GenericParser<Input, Output> {
    fun repExactly(n: UInt): GenericParser<Input, List<Output>> {
        TODO()
    }
}

fun box(): String {
    val parser = object : ByteArrayParser<String> {}
    return "OK"
}