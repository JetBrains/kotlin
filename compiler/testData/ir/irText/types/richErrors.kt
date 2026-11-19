// LANGUAGE: +RichErrors
// SKIP_NEW_KOTLIN_REFLECT_COMPATIBILITY_CHECK
//   ^^^ KT-89093 Rich Errors reflection support
// SKIP_GENERATING_KLIB
//   ^^^ KT-91653 IR: add serialization support for rich errors

@Target(AnnotationTarget.TYPE) annotation class Ann1
@Target(AnnotationTarget.TYPE) annotation class Ann2
@Target(AnnotationTarget.TYPE) annotation class Ann3

error object Err1
error object Err2

fun foo(x: Int | Err1 | Err2) {}
fun <T: NonError?> bar(): T & Any | Err1 = TODO()
fun baz(): @Ann1 (@Ann2 Err1 | Err2) = TODO()
fun foobar(): @Ann1 (@Ann2 String | @Ann3 Err1) = TODO()
