class Incomplete {
    fun get{}
}


fun main(args: Array<String>) {
    var incomplete: Incomplete = Incomplete(1)
    <expr>incomplete[0]</expr> += 1
}
