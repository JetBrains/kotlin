fun outer() {
    fun local() {

    }

    @Suppress("UnusedSymbol")
    fun localNoWarn() {

    }
}

@Suppress("UnusedSymbol")
fun otherFun() {
    fun localNoWarn() {

    }
}