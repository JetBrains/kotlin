// "Replace array of boxed with array of primitive" "true"
annotation class SuperAnnotation(
        val s: <caret>Array<Short>,
        val str: Array<String>
)