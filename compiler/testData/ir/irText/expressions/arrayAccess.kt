val p = 0
fun foo() = 1

fun test(a: IntArray) =
    a[0] + a[p] + a[foo()]

fun test1(a: IntArray) {
    a[0] += 1
    a[1] -= 2
    a[2] *= 3
    a[3] /= 4
    a[4] %= 5
    a[5] = 6
    a[6]++
    ++a[7]
    a[8]--
    --a[9]
}
