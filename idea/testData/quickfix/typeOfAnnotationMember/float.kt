// "Replace array of boxed with array of primitive" "true"
annotation class SuperAnnotation(
        val f: <caret>Array<Float>,
        val str: Array<String>
)