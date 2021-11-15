// WITH_STDLIB

class Environment(
    val fieldAccessedInsideChild: Int,
    val how: Environment.() -> Unit
)
fun box(): String {
    Environment(
        3,
        {
            class Child {
                val a = fieldAccessedInsideChild
            }
            class Parent {
                val children: List<Child> =
                    (0..4).map { Child() }
            }
        }
    )

    return "OK"
}