interface A

fun <T> test(t: T) {
    @Denotable("T") t
    if (t != null) {
        (@Denotable("T!!") t).equals("")
    }
    val outs = take(getOutProjection())
    @Denotable("A") outs

    val ins = take(getInProjection())
    @Denotable(kotlin.Any?) ins
}

fun getOutProjection(): MutableList<out A> {
    TODO()
}

fun getInProjection(): MutableList<in A> {
    TODO()
}

fun <T> take(l: MutableList<T>): T {
    TODO()
}
