fun foo() {
    fun outerFunction(): Boolean = true
    var outerVariable = true
    do {
        var innerVariable = true
        fun innerFunction(): Boolean = true
    } while (<expr>outerVariable || outerFunction() || innerVariable || innerFunction()</expr>)

    var unrelatedVariable = false
    <expr>fun unrelatedFunction() {}</expr>
}
