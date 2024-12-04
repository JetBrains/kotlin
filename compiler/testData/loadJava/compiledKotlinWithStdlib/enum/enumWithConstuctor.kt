// ALLOW_AST_ACCESS
package test

enum class En(val b: Boolean = true, val i: Int = 0) {
    E1(),
    E2(true, 1),
    E3(i = 2)
}
