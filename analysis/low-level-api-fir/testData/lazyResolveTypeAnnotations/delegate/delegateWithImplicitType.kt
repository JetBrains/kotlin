// MEMBER_NAME_FILTER: implicitType

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)
const val prop = 0

class Der<caret>ived(i: OriginalInterface) : OriginalInterface by i

interface OriginalInterface {
    @Anno("implicitType $prop")
    fun <@Anno("type param $prop") F : @Anno("bound $prop") List<@Anno("nested bound $prop") List<@Anno("nested nested bound $prop") String>>> @receiver:Anno("receiver annotation: $prop") @Anno("receiver type $prop") Collection<@Anno("nested receiver type $prop") List<@Anno("nested nested receiver type $prop")String>>.implicitType(@Anno("parameter annotation $prop") param: @Anno("parameter type $prop") ListIterator<@Anno("nested parameter type $prop") List<@Anno("nested nested parameter type $prop")String>>) = explicitType()
}

fun explicitType(): @Anno("explicitType return type $prop") List<@Anno("explicitType nested return type $prop") List<@Anno("explicitType nested nested return type $prop") Int>> = 1
