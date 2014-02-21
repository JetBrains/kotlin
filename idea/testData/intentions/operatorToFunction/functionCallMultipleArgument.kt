fun foo(i: Any?, j: Any?) {
    fun bar(i: Any?, j: Any?) : Boolean {
        return true
    }

    bar<caret>(i, j)
}
