@Target(AnnotationTarget.TYPE)
annotation class Anno1

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs1(val x: String)

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno2

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class AnnoWithArgs2(val x: String)

@Target(AnnotationTarget.TYPE)
annotation class Anno3

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs3(val x: String)

class Inv<@Anno2 @AnnoWithArgs2("") T>(val value: @Anno3 @AnnoWithArgs3("") T?)

fun foo(i: Inv<out @Anno1 @AnnoWithArgs1("") CharSequence>) {
    val x = <expr>i.value</expr>
}
