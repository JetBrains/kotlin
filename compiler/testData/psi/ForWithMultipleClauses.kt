fun main() {
    for (i in x, j in y) foo(i, j)

    for (i in x, j in y) {
        foo(i, j)
    }

    for (i in x, j: Int in y) foo(i, j)

    for ((i, j) in x, var (k: Int, l: String) in y) foo(i, j)
}