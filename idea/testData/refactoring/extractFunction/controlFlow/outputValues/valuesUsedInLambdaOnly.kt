class T(val n: Int)

// SIBLING:
fun foo() {
    <selection>bar { t ->
        val k = 1
        t.n + k + 1
    }</selection>
}

fun bar(f: (T) -> Int) {

}