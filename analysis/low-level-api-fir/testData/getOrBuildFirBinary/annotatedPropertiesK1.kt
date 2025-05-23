// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: Foo
// LANGUAGE_VERSION: 1.9
// API_VERSION: 1.9
// ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING

// FILE: Foo.kt
// K1 metadata : default accessors, K2 metadata : non-default accessors
class Foo(
    @get:Anno val withDefaultGetter: Int,
    @set:Anno var withDefaultSetter: String,
    @get:Anno @set:Anno var both: String,
    @get:MyAnnotation @set:MyAnnotation2 @setparam:MyAnnotation3 @property:MyAnnotation4 @field:MyAnnotation5 @param:MyAnnotation6 var allAnnotations: String,
) {
    fun @receiver:MyAnnotation7 @MyAnnotation8 Int.memberFunctionWithReceiver() = Unit
    fun @receiver:MyAnnotation7 @MyAnnotation8 Int?.memberFunctionWithReceiverWithNullableReceiver(l: Long) = Unit
    var @receiver:MyAnnotation7 @MyAnnotation8 Int.memberPropertyWithReceiver
        get() = Unit
        set(value) {}

    @Anno
    internal var withCustomSetter = "OK"
        set(value) {}

    @Anno
    internal var withCustomGetter = "OK"
        get() = "KO"

    @Anno
    internal var custom = "OK"
        get() = field
        set(value) {
            field = value
        }

    @Anno
    internal var variable = "OK"
}

// FILE: Annotations.kt
annotation class Anno
annotation class MyAnnotation
annotation class MyAnnotation2
annotation class MyAnnotation3
annotation class MyAnnotation4
annotation class MyAnnotation5
annotation class MyAnnotation6
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class MyAnnotation7
@Target(AnnotationTarget.TYPE)
annotation class MyAnnotation8
