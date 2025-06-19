package foo

@Target(AnnotationTarget.TYPE)
annotation class MyAnno(val s: String)

fun f<caret>oo(): @MyAnno("str" + "1") String {

}
