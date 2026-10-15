package one

@Target(AnnotationTarget.TYPE)
annotation class TypeAnno(val i: Int)

fun foreign(): @TypeAnno({
    fun localInForeign() = 1
    localInForeign()
}) Int = 0

val fromFor<caret>eign = foreign()
