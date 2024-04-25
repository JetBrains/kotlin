// DUMP_CFG

fun foo(): Int = 1

fun test(x: Any, y: String = x as String, z: Int = run { foo() }) {
    foo()
}
