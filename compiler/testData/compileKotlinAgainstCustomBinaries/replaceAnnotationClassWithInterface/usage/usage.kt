package test

@Ann("class")
class Test {
    @Ann("function")
    fun foo(@Ann("parameter") s: @Ann("parameter type") String): @Ann("return type") String = s
}
