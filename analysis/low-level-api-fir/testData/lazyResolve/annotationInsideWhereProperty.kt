@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)

interface One
interface Two

val <T> T.fo<caret>o where T : One, T : @Anno("str") Two get() = this