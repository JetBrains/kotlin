package one

annotation class Anno(val i: Int)

@Anno({
    fun local() = 1
    local()
})
v<caret>al (@Anno({ 2 }) first, second) = 1 to 2
