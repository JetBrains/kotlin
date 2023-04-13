val p = 0
fun foo() = 1

fun test(a: IntArray) =
        a[0] + a[p] + a[foo()]
