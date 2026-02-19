// QUERY: classIds
fun topLevel() {
    @MyAnno
    fun f<caret>oo(i: Int) = 1
}

annotation class MyAnno