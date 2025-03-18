// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75242

annotation class Decl

fun test(x: Int) {
    if (x > 10)
        @all:Decl { }

    for (i in 1..10) @field:Decl { }

    when (x) {
        1 -> @setparam:Decl { "" }
    }
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class Source

fun test(){
    @all:Source
    when { else -> {} }

    @get:Source
    while (true) { break }

    @receiver:Source
    for (i in 1..10) {}
}
