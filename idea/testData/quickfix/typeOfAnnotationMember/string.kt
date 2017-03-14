// "Replace array of boxed with array of primitive" "false"
// ACTION: Convert to secondary constructor
annotation class SuperAnnotation(
        val str: <caret>Array<String>
)