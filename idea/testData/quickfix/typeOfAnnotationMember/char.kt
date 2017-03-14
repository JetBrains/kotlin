// "Replace array of boxed with array of primitive" "true"
annotation class SuperAnnotation(
        val c: <caret>Array<Char>,
        val str: Array<String>
)