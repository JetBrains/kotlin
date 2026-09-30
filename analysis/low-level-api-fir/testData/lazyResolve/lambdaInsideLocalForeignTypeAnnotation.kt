package one

@Target(AnnotationTarget.TYPE)
annotation class TypeAnno(val i: Int)

fun foreign() = object {
    fun member(): @TypeAnno({
        fun localInMember() = 1
        localInMember()
    }) Int = 0
}.member()

val fromFor<caret>eign = foreign()
