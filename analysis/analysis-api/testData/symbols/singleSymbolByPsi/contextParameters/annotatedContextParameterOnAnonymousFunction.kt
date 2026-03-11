
@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

fun foo() {
    val a = context(para<caret>meter1 : @Anno("1" + "2") String, parameter2: List<@Anno("str") Int>) fun() {

    }
}
