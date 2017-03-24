// "Replace array of boxed with array of primitive" "false"
// ACTION: Convert to secondary constructor
// ACTION: Move to class body
annotation class SuperAnnotation(
        val str: <caret>Array<String>
)