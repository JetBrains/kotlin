// FIR_IDENTICAL
// FIR_DUMP

annotation class NoTarget

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param

@Target(AnnotationTarget.PROPERTY)
annotation class Prop

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class Both

data class Foo(
    @NoTarget @Param @Prop @Both val p1: Int,
    @param:NoTarget @param:Both val p2: String,
    @property:NoTarget @property:Both val p3: Boolean,
)