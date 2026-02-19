@Target(AnnotationTarget.FIELD)
annotation class FieldAnnotation(val i: Int)

@Repeatable
annotation class Anno(val s: String)

class Sub(
    @FieldAnnotation(konstant)
    @field:Anno("field$stringConstant")
    @property:Anno("property$stringConstant")
    @get:Anno("get$stringConstant")
    @set:Anno("set$stringConstant")
    @setparam:Anno("setparam$stringConstant")
    @param:Anno("param$stringConstant")
    var pr<caret>op: Int,
)

const val konstant = 0
const val stringConstant = "str ${1}"