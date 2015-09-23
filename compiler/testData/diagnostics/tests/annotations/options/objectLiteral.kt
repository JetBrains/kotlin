@Target(AnnotationTarget.CLASS)
annotation class Ann

open class My

fun foo(): My {
    return (@Ann object: My() {})
}