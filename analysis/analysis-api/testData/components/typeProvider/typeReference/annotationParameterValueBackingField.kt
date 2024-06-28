@Target(AnnotationTarget.FIELD)
annotation class Anno

class Sub(
    @An<caret>no
    @JvmField
    var x: Int
)