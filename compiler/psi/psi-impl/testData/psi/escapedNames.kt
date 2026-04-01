// LANGUAGE: +ContextParameters +NestedTypeAliases
package `one two`.`three four`

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class `Annotation Class`

@`Annotation Class`
class `Top Level Class`<`Type Parameter` : `Top Level Object`.`Member Level Interface`> {
    context(`context name`: `Member Level TypeAlias`<`Type Parameter`>)
    fun `Top Level Object`.`Member Level Interface`.`member function`(`parameter name`: `Top Level Class`<`Type Parameter`>) {}
    var `Member Level TypeAlias`<`Type Parameter`>.`member property`: `Top Level Class`<`Type Parameter`>? get() = null
        set(value) {}

    @`Annotation Class`
    typealias `Member Level TypeAlias`<T> = `Top Level Class`<T>

    constructor(`parameter name`: `Top Level Object`)
}

typealias `Top Level TypeAlias`<`Type Parameter`> = `Top Level Class`<`Type Parameter`>

interface `Top Level Interface`

object `Top Level Object` : `Top Level Interface` {
    interface `Member Level Interface` {

    }
}

@`Annotation Class`
enum class `Enum Class` {
    @`Annotation Class`
    `Enum Entry`;
}
