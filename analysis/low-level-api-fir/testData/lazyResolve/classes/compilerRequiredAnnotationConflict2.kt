import kotlin.annotation.AnnotationTarget.FIELD

object Some {
    @Target(FIELD)
    annotation class An<caret>n

    const val FIELD = ""
}
