// FILE: MyAnnotation.kt
annotation class MyAnnotation
// FILE: MyAnnotation2.kt
annotation class MyAnnotation2
// FILE: MyAnnotation3.kt
annotation class MyAnnotation3
// FILE: MyAnnotation4.kt
annotation class MyAnnotation4
// FILE: MyAnnotation5.kt
annotation class MyAnnotation5
// FILE: MyAnnotation6.kt
annotation class MyAnnotation6
// FILE: MyAnnotation7.kt
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class MyAnnotation7
// FILE: MyAnnotation8.kt
@Target(AnnotationTarget.TYPE)
annotation class MyAnnotation8

// FILE: Test.kt
class Test(@get:MyAnnotation @set:MyAnnotation2 @setparam:MyAnnotation3 @property:MyAnnotation4 @field:MyAnnotation5 @param:MyAnnotation6 var bar: String) {
    fun @receiver:MyAnnotation7 @MyAnnotation8 Int.fooF() = Unit
    fun @receiver:MyAnnotation7 @MyAnnotation8 Int?.fooWithNullableReceiver(l: Long) = Unit
    var @receiver:MyAnnotation7 @MyAnnotation8 Int.fooP
        get() = Unit
        set(value) {}
}
