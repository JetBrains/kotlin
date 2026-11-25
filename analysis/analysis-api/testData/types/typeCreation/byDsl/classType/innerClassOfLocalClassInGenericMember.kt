class A<AA> {
    fun <F> foo(
        ww: Int,
        vv: String,
    ) {
        class Local<L> {
            inner class Inner<I>
        }

        val xx = Local<Int>().Inner<String>()

        x<caret_type>x.toString()
        w<caret_int>w.toString()
        v<caret_string>v.toString()
    }
}
