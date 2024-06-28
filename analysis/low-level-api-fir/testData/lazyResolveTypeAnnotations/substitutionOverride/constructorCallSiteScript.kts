// BODY_RESOLVE
@Target(AnnotationTarget.TYPE)
annotation class Anno(val str: String)

open class P1<T1> {
    inner class P2<T2>(i: String) : @Anno("P1 super type") P1<@Anno("nested P1 super type") T1>() {
        constructor() : this("OK") {}
    }

    fun <T2> crea<caret>teP2(): @Anno("return type") P2<@Anno("nested return type") T2> = P2()
}
