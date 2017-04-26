// "Replace array of boxed with array of primitive" "false"
// ACTION: Move to class body
annotation class SuperAnnotation(
        val str: <caret>Array<String>
)