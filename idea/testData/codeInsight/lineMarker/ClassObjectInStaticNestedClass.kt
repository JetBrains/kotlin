trait <lineMarker></lineMarker>TestTrait {
    fun <lineMarker></lineMarker>test()
}

class A {
    class B {
        class object : TestTrait { // TODO: No line marker
            override fun <lineMarker descr="<b>internal</b> <b>open</b> <b>fun</b> test(): jet.Unit <i>defined in</i> A.B.&lt;class-object-for-B&gt;<br/>implements<br/><b>internal</b> <b>abstract</b> <b>fun</b> test(): jet.Unit <i>defined in</i> TestTrait"></lineMarker>test() {
            }
        }
    }
}