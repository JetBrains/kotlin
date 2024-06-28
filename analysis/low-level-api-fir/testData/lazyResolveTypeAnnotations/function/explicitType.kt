package lowlevel

@Target(
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
)
annotation class Anno(val position: String)

const val prop = "str"

interface A

@Anno("function $prop")
fun <@Anno("type parameter $prop") T : @Anno("bound type $prop") List<@Anno("nested bound type $prop") String>> @receiver:Anno("receiver $prop") @Anno("receiver type $prop")
List<@Anno("nested receiver type $prop") List<@Anno("nested nested receiver type $prop") Int>>.func<caret>tion(@Anno("parameter $prop") param: @Anno("parameter type $prop") Collection<@Anno("nested parameter type $prop") List<@Anno("nested nested parameter type $prop") String>> = 1): @Anno("return type $prop") List<@Anno("nested return type $prop") List<@Anno("nested nested return type $prop") Int>>
        where T : @Anno("constraint type $prop") Collection<@Anno("nested constraint type $prop") Int> = 1