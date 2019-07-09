// "Replace array of boxed with array of primitive" "false"
// ERROR: Invalid type of annotation member
// ACTION: Introduce import alias
// ACTION: Put parameters on one line
annotation class SuperAnnotation(
        val foo: <caret>Array<*>,
        val str: Array<String>
)