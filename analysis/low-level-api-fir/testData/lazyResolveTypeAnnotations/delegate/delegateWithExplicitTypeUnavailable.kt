// MEMBER_NAME_FILTER: explicitType

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

class Der<caret>ived(i: OriginalInterface) : OriginalInterface by i

interface OriginalInterface {
    @Anno("explicitType $prop")
    fun <@Anno("type param $prop") F : @Anno("bound $prop") List<@Anno("nested bound $prop") List<@Anno("nested nested bound $prop") String>>> @receiver:Anno("receiver annotation: $prop") @Anno("receiver type $prop") Collection<@Anno("nested receiver type $prop") List<@Anno("nested nested receiver type $prop")String>>.explicitType(@Anno("parameter annotation $prop") param: @Anno("parameter type $prop") ListIterator<@Anno("nested parameter type $prop") List<@Anno("nested nested parameter type $prop")String>>): @Anno("explicitType return type $prop") List<@Anno("explicitType nested return type $prop") List<@Anno("explicitType nested nested return type $prop") Int>> = 1
    companion object {
        private const val prop = 0
    }
}
