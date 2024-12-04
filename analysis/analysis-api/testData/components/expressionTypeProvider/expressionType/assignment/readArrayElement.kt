class FOO {
    var str: String = ""
    operator fun plusAssign(tk: String?) {
        str += tk
    }
}

fun main(args: Array<String?>) {
    val foo: FOO = FOO()
    if (args.size > 2)
        foo += <expr>args[2]</expr>
    else
        foo += foo.toString()
}