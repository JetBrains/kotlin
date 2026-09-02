// FILE: declarations.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno(val number: Int)

const val C = 1

// FILE: main.kt
fun test(value: <expr>Map<List<@Anno(C) String>, (@Anno(C) Int) -> @Anno(C + 1) String></expr>) {}
