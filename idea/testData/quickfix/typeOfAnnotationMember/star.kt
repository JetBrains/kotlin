// "Replace array of boxed with array of primitive" "false"
// ACTION: Move to class body
// ERROR: Invalid type of annotation member
annotation class SuperAnnotation(
        val foo: <caret>Array<*>,
        val str: Array<String>
)