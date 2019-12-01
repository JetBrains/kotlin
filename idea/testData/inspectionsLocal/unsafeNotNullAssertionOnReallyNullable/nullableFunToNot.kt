// SKIP_ERRORS_AFTER

class A {
    fun unsafeCall(){}
}

class B {
    fun returnA(): A?{
        return null
    }
}

fun unsafeFoo() {
    val b = B()
    b.returnA()<caret>!!.unsafeCall()
}