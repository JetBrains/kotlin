@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE) annotation class base

fun foo1(vararg ints: Int) {}
fun foo2(@base vararg ints: @base Int) {}
