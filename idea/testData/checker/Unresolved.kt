// IGNORE_FIR

package unresolved

class Pair<A, B>(<warning>a</warning>: A, <warning>b</warning>: B)

fun testGenericArgumentsCount() {
    val <warning>p1</warning>: Pair<error><Int></error> = Pair(2, 2)
    val <warning>p2</warning>: <error>Pair</error> = Pair(2, 2)
}

fun testUnresolved() {
    if (<error>a</error> is String) {
        val <warning>s</warning> = <error>a</error>
    }
    <error>foo</error>(<error>a</error>)
    val s = "s"
    <error>foo</error>(s)
    foo1(<error>i</error>)
    s.<error>foo</error>()

    when(<error>a</error>) {
        is Int -> <error>a</error>
        is String -> <error>a</error>
        else -> <error>a</error>
    }

    for (j in <error>collection</error>) {
       var i: Int = <error>j</error>
       i += 1
       foo1(<error>j</error>)
    }
}

fun foo1(<warning>i</warning>: Int) {}
