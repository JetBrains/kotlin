// LANGUAGE_VERSION: 1.4
// PROBLEM: none

@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class Ann(val n: Int)

@get:Ann(10)<caret>
val a: String
    @Ann(20) get() = "foo"