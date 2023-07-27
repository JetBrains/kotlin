package one.two

fun reso<caret>lveMe() {
    @Deprecated(message = "", replaceWith = ReplaceWith(expression = "abc"), level = DeprecationLevel.ERROR)
    class LocalClass

    @Target(AnnotationTarget.TYPE)
    class B
}