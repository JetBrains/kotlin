// "Replace array of boxed with array of primitive" "false"
// ACTION: Convert to secondary constructor
// ERROR: Invalid type of annotation member
annotation class SuperAnnotation(
        val foo: <caret>Array<*>,
        val str: Array<String>
)