private typealias A = String.(Int) -> Int

private fun b(a: A) {
    "b".a(1)
}

fun main() {
    b {
        length + it
    }
}
