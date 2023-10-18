@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)

interface One
interface Two

fun <T> f<caret>oo(t: T) where T : One, T : @Anno("str") Two = t