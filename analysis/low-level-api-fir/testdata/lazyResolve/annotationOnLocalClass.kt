package one.two

annotation class AnotherAnnotation
annotation class KotlinAnnotation(val s: AnotherAnnotation)

fun resol<caret>veMe() {
    @KotlinAnnotation(AnotherAnnotation())
    class LocalClass
}