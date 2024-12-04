// BODY_RESOLVE
annotation class Anno(val position: String)

fun fo<caret>o() {
    class OriginalClass {
        val prop = 0

        @Anno("function $prop")
        fun <@Anno("type param $prop") F : @Anno("bound $prop") List<@Anno("nested bound $prop") List<@Anno("nested nested bound $prop") String>>> @receiver:Anno("receiver annotation: $prop") @Anno("receiver type $prop") Collection<@Anno("nested receiver type $prop") List<@Anno("nested nested receiver type $prop")String>>.explicitType(@Anno("parameter annotation $prop") param: @Anno("parameter type $prop") ListIterator<@Anno("nested parameter type $prop") List<@Anno("nested nested parameter type $prop")String>>): @Anno("explicitType return type $prop") List<@Anno("explicitType nested return type $prop") List<@Anno("explicitType nested nested return type $prop") Int>> = 0
    }
}
