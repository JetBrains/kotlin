// !DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB

@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

interface OriginalInterface {
    @Anno("explicitType $prop")
    fun <@Anno("type param $<!UNRESOLVED_REFERENCE!>prop<!>") F : @Anno("bound $<!UNRESOLVED_REFERENCE!>prop<!>") List<@Anno("nested bound $<!UNRESOLVED_REFERENCE!>prop<!>") List<@Anno("nested nested bound $<!UNRESOLVED_REFERENCE!>prop<!>") String>>> @receiver:Anno("receiver annotation: $prop") @Anno("receiver type $prop") Collection<@Anno("nested receiver type $prop") List<@Anno("nested nested receiver type $prop")String>>.explicitType(@Anno("parameter annotation $prop") param: @Anno("parameter type $prop") ListIterator<@Anno("nested parameter type $prop") List<@Anno("nested nested parameter type $prop")String>>): @Anno("explicitType return type $prop") List<@Anno("explicitType nested return type $prop") List<@Anno("explicitType nested nested return type $prop") Int>> = emptyList()
    val <@Anno("type param $<!UNRESOLVED_REFERENCE!>prop<!>") F : @Anno("bound $<!UNRESOLVED_REFERENCE!>prop<!>") Number> F.explicitType: @Anno("bound $prop") Int get() = 1

    companion object {
        private const val prop = 0
    }
}
