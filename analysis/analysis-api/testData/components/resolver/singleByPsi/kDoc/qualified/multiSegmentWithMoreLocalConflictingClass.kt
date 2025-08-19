interface A {
    companion object {
        val BB: Int = 1
    }
}

class Usage1 {
    class A

    /**
     * [A.B<caret>B]
     */
    fun foo() {}
}