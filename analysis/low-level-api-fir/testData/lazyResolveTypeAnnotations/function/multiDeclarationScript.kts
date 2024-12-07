package util

annotation class Anno(val position: String)
const val constant = "s"
data class Pair(val a: @Anno("a type $constant") List<@Anno("a nested type $constant") Collection<@Anno("a nested nested type $constant") String>>?, val b: @Anno("b type $constant") Array<@Anno("b nested type $constant") List<@Anno("b nested nested type $constant") Int>>?)
fun bar(): @Anno("b type $constant") List<@Anno("b nested type $constant") Pair> = null!!
fun <T> withLambda(lambda: (Pair) -> T): T {}

fun fo<caret>o() = withLambda { _ ->
    val prop = "str"
    @Anno("near for $prop")
    for (@Anno("for $prop") i in 1..100) {}
    for (@Anno("second for $prop") (x, @Anno("destructuring in for $prop") y) in bar()) {}
    withLambda { (@Anno("lambda a $prop") a, @Anno("lambda b $prop") b) ->

    }

    @Anno("destr $prop")
    val (@Anno("a $prop") a, @Anno("b $prop") b) = Pair(null, null)
    b
}
