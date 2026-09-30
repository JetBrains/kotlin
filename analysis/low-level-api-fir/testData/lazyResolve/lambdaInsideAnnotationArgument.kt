package one

annotation class Anno(val i: Int)

@Target(AnnotationTarget.TYPE)
annotation class TypeAnno(val i: Int)

@Anno({
    fun local() = 1
    val x = local()
    x
})
fun fo<caret>o(@Anno(fun(): Int { val y = 2; return y }) param: @TypeAnno({ 3 }) Int) {}
