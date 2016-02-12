class C(var v: Int) {
    fun foo() {
        print(<caret>v)
    }
}

//INFO: <b>public</b> <b>final</b> <b>var</b> v: Int <i>defined in</i> C
