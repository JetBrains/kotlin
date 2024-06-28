package myPack

@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE,
)
annotation class Anno(val position: String)
const val prop = "str"

@Anno("property $prop")
var <@Anno("type parameter $prop") T : @Anno("bound $prop") List<@Anno("nested bound $prop") String>> @receiver:Anno("receiver $prop") @Anno("receiver type $prop") List<@Anno("nested receiver type $prop")>.vari<caret>ableToResolve: @Anno("return type $prop") Collection<@Anno("nested return type $prop") List<@Anno("nested nested return type $prop") Int>>
    @Anno("getter $prop")
    get() = "str"
    @Anno("setter $prop")
    set(@Anno("setter parameter $prop") value) = Unit
