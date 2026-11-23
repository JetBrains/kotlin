// LANGUAGE: +MultiPlatformProjects
fun foo(a: Any): Lazy<Int> = lazy { 1 }

class C {
    val p<caret>p by foo(@Suppress("") val x by lazy { 1 })
}
