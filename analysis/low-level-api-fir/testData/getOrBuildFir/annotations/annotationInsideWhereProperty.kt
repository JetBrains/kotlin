// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtAnnotationEntry

@Target(AnnotationTarget.TYPE)
annotation class Anno(val s: String)

interface One
interface Two

val <T> T.foo where T : One, T : <expr>@Anno("str")</expr> Two get() = this