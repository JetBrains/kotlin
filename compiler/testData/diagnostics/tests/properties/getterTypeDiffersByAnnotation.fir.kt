@Target(AnnotationTarget.TYPE)
annotation class TypeAnn(val name: String)

class Kotlin {
    val annotatedGetter: Int
        get(): <!WRONG_GETTER_RETURN_TYPE!>@TypeAnn("1") Int<!> = 123

    val unannotatedGetter: @TypeAnn("1") Int
        get(): <!WRONG_GETTER_RETURN_TYPE!>Int<!> = 123
}
