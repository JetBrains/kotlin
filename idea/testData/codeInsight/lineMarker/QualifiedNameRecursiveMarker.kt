package foo

class Bar {
    fun foobar() {
        <lineMarker descr="Recursive call on foo.Bar.foobar"></lineMarker>foobar()
    }
}