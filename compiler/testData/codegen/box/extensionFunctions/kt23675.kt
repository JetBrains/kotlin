//WITH_RUNTIME

// IGNORE_BACKEND: JS_IR

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