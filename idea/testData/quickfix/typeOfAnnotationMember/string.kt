// "Replace array of boxed with array of primitive" "false"
// ACTION: Put parameters on one line
// ACTION: Convert to vararg parameter (may break code)
annotation class SuperAnnotation(
        val str: <caret>Array<String>
)