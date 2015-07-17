import testing.JavaClass

fun usages(javaClass: JavaClass) {
    javaClass.something = javaClass.somethingNew + 1
    javaClass.somethingNew++
}