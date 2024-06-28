// BODY_RESOLVE
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

open class A<T>

fun fo<caret>o() {
    val localProp = 1
    @Anno("class $localProp")
    class OriginalClass<@Anno("type param $localProp") T : @Anno("bound $localProp") List<@Anno("nested bound $localProp") Int>> : @Anno("super type $localProp") A<@Anno("nested super type $localProp") List<@Anno("nested nested super type $localProp") Int>>() {
        val prop = 0

        @Anno("class $prop")
        class InnerClass<@Anno("type param $prop") T : @Anno("bound $prop") List<@Anno("nested bound $prop") Int>> : @Anno("super type $prop") A<@Anno("nested super type $prop") List<@Anno("nested nested super type $prop") Int>>()
    }
}
