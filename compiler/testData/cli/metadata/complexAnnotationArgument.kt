@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION,
        AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.BINARY)
annotation class Ann(val str: String)

@Ann("a" + "b")
val a: @Ann("a" + "b") Int = 1

@Ann("a" + "b")
class B {
    @Ann("a" + "b")
    fun foo(@Ann("a" + "b") x: @Ann("a" + "b") Int) {}
}
