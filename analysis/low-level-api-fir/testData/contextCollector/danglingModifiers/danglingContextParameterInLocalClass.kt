@Target(AnnotationTarget.TYPE)
annotation class Anno(val position: String)

fun foo(i: Int) {
    class Foo {
        context(<expr> parameter1: @Anno("1" + "2") String</expr>, parameter2: List<@Anno("str") Int>)
    }
}
