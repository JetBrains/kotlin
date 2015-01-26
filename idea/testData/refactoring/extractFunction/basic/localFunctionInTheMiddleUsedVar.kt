fun test(): Int {
    <selection>val foo = 1
    fun bar() = 2

    return when (foo) {
        1 -> 1
        else -> 2
    }</selection>
}