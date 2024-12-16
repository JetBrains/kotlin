@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class Anno

class UnresolvedArgument(@Anno(BLA) val s: Int)

class WithoutArguments(@Deprecated val s: Int)

fun test() {
    UnresolvedArgument(3)
    WithoutArguments(0)
}
