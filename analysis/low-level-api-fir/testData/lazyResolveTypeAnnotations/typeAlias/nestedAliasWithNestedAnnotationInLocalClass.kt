// BODY_RESOLVE
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val position: String)

fun f<caret>oo() {
    class OriginalClass<T> {
        val prop = 0

        @Anno("alias $prop")
        typealias NestedTypeAlias <@Anno("type param $prop") A : @Anno("bound $prop") Number> = @Anno("type $prop") OriginalClass<A>
    }
}
