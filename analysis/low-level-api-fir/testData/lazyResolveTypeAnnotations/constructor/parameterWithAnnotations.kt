@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

const val prop = "str"

class A {
    cons<caret>tructor(param: @Anno("parameter type $prop") List<@Anno("nested parameter type $prop") Collection<@Anno("nested nested parameter type $prop") String>> = @Anno("defaultValue $prop") fun(i: @Anno("anonymousFunction parameter type $prop") Int): @Anno("anonymousFunction return type $prop") Int {})
}
