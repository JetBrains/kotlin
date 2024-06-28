// FIR_IDENTICAL
@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

class Kotlin {
    val annotatedGetter: Int
        get(): @TypeAnn("1") Int = 123

    val unannotatedGetter: @TypeAnn("1") Int
        get(): Int = 123
}