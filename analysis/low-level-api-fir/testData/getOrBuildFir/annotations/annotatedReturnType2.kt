// SKIP_WHEN_OUT_OF_CONTENT_ROOT

// FILE: first.kt

fun test() {
    withoutAnno()
    <expr>withAnno()</expr>
}

// FILE: second.kt

@Target(AnnotationTarget.TYPE)
annotation class Anno

fun withAnno(): @Anno String = "foo"
fun withoutAnno(): String = "bar"