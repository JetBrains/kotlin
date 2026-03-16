@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Abc

fun <@Abc T> f(x: T) {
    fun <@Abc U> g(a: T, b: U) {}
}

// 2 @LAbc;\(\) : METHOD_TYPE_PARAMETER 0, null
// 1 @LAbc;\(\) : METHOD_TYPE_PARAMETER 1, null
