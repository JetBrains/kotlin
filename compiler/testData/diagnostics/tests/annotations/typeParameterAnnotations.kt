// !DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

interface OriginalInterface {
    @Anno("explicitType $prop")
    fun <@Anno("type param $prop") F : @Anno("bound $prop") List<@Anno("nested bound $prop") List<@Anno("nested nested bound $prop") String>>> @receiver:Anno("receiver annotation: $prop") <!REPEATED_ANNOTATION!>@Anno("receiver type $prop")<!> Collection<@Anno("nested receiver type $prop") List<@Anno("nested nested receiver type $prop")String>>.explicitType(@Anno("parameter annotation $prop") param: @Anno("parameter type $prop") ListIterator<@Anno("nested parameter type $prop") List<@Anno("nested nested parameter type $prop")String>>): @Anno("explicitType return type $prop") List<@Anno("explicitType nested return type $prop") List<@Anno("explicitType nested nested return type $prop") Int>> = emptyList()
    val <@Anno("type param $prop") F : @Anno("bound $prop") Number> @receiver:Anno("receiver annotation: $prop") <!REPEATED_ANNOTATION!>@Anno("receiver type $prop")<!> F.explicitType: @Anno("bound $prop") Int get() = 1

    companion object {
        private const val prop = 0
    }
}
