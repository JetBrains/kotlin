fun nonExhaustiveInt(x: Int) = <error descr="[NO_ELSE_IN_WHEN] when expression must be exhaustive, add necessary 'else' branch">when</error>(x) {
    0 -> false
}

fun nonExhaustiveBoolean(b: Boolean) = <error descr="[NO_ELSE_IN_WHEN] when expression must be exhaustive, add necessary 'true' branch or 'else' branch instead">when</error>(b) {
    false -> 0
}

enum class Color { 
    RED,
    GREEN,
    BLUE
}

fun nonExhaustiveEnum(c: Color) = <error descr="[NO_ELSE_IN_WHEN] when expression must be exhaustive, add necessary 'RED', 'BLUE' branches or 'else' branch instead">when</error>(c) {
    Color.GREEN -> 0xff00
}

fun nonExhaustiveNullable(c: Color?) = <error descr="[NO_ELSE_IN_WHEN] when expression must be exhaustive, add necessary 'GREEN', 'null' branches or 'else' branch instead">when</error>(c) {
    Color.RED -> 0xff
    Color.BLUE -> 0xff0000
}

fun whenOnEnum(c: Color) {
    <warning descr="[NON_EXHAUSTIVE_WHEN] when expression on enum is recommended to be exhaustive, add 'RED' branch or 'else' branch instead">when</warning>(c) {
        Color.BLUE -> {}
        Color.GREEN -> {}
    }
}

sealed class Variant {
    object Singleton : Variant()
   
    class Something : Variant()

    object Another : Variant()
}

fun nonExhaustiveSealed(v: Variant) = <error descr="[NO_ELSE_IN_WHEN] when expression must be exhaustive, add necessary 'is Something', 'Another' branches or 'else' branch instead">when</error>(v) {
    Variant.Singleton -> false
}

sealed class Empty

fun nonExhaustiveEmpty(e: Empty) = <error descr="[NO_ELSE_IN_WHEN] when expression must be exhaustive, add necessary 'else' branch">when</error>(<warning>e</warning>) {}

fun nonExhaustiveNullableEmpty(e: Empty?) = <error descr="[NO_ELSE_IN_WHEN] when expression must be exhaustive, add necessary 'else' branch">when</error>(<warning>e</warning>) {}
