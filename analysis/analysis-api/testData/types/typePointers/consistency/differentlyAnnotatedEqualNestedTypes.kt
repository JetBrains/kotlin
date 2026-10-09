@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)

class Three<A, B, C>

fun test(value: <expr>Three<List<@Anno(1) String>, List<String>, List<@Anno(2) String>></expr>) {}
