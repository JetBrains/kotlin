// RESOLVE_SCRIPT
// BODY_RESOLVE
package util

annotation class Anno(val str: String)
const val constant = "s"
data class Pair(val a: @Anno("a type $constant") List<@Anno("a nested type $constant") Collection<@Anno("a nested nested type $constant") String>>?, val b: @Anno("b type $constant") Array<@Anno("b nested type $constant") List<@Anno("b nested nested type $constant") Int>>?)
const val prop = "str"

if (true) {
    @Anno("destr 1 $prop")
    val (@Anno("a $prop") a, @Anno("b $prop") b) = Pair(null, null)

    @Anno("destr 1 $prop")
    val (@Anno("c $prop") c, @Anno("d $prop") d) = Pair(null, null)
}

fun foo() {}