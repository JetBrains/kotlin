// IS_APPLICABLE: false
@Target(AnnotationTarget.TYPE)
annotation class Foo

fun foo(): @Foo Int<caret> = 1
