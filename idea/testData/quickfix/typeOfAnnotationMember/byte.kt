// "Replace array of boxed with array of primitive" "true"
annotation class SuperAnnotation(
        val b: <caret>Array<Byte>,
        val str: Array<String>
)