// "Replace array of boxed with array of primitive" "false"
// ERROR: Invalid type of annotation member
annotation class SuperAnnotation(
        val foo: <caret>Array<*>,
        val str: Array<String>
)