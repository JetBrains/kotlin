@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class a
interface A
interface B

fun () {}
fun A.() {}

@a fun () {}
fun @a A.() {}

class Outer {
    fun () {}
    fun B.() {}

    @a fun () {}
    fun @a A.() {}
}

fun outerFun() {
    fun () {}
    fun B.() {}

    @a fun () {}
    fun @a A.() {}
}