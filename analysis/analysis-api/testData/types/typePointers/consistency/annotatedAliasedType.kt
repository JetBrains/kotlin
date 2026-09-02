// WITH_STDLIB

@Target(AnnotationTarget.TYPE)
annotation class Anno1

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs1(val x: String)

@Target(AnnotationTarget.TYPE)
annotation class Anno2

@Target(AnnotationTarget.TYPE)
annotation class AnnoWithArgs2(val x: String)

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno3

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class AnnoWithArgs3(val x: String)

typealias MyList<@Anno3 @AnnoWithArgs3("") T> = List<T>
typealias MyString = String

fun test(value: <expr>@Anno1 @AnnoWithArgs1("") MyList<@Anno2 @AnnoWithArgs2("") MyString></expr>) {}
