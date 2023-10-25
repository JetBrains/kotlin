// MEMBER_NAME_FILTER: prop

@Target(AnnotationTarget.FIELD)
annotation class FieldAnnotation(val i: Int)

@Repeatable
annotation class Anno(val s: String)

class <caret>Sub(
    @FieldAnnotation(konstant)
    @field:Anno("field$stringConstant")
    @property:Anno("property$stringConstant")
    @get:Anno("get$stringConstant")
    @set:Anno("set$stringConstant")
    @setparam:Anno("setparam$stringConstant")
    @param:Anno("param$stringConstant")
    var prop: Int,
)

const val konstant = 0
const val stringConstant = "str ${1}"