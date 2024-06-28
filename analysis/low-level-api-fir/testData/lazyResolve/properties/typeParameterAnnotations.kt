@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

interface OriginalInterface {
    val <@Anno("type param $prop") F : @Anno("bound $prop") Number> explic<caret>itType: @Anno("bound $prop") Int get() = 1

    companion object {
        private const val prop = 0
    }
}
