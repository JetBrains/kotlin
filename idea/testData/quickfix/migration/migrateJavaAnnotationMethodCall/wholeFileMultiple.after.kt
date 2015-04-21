// "Replace deprecated method calls with property access in current file" "true"
// WITH_RUNTIME

class A {
    fun foo() {}
}

fun bar(x: Int) {
    javaClass<A>().getAnnotation(javaClass<Ann>()).args[0]
    javaClass<A>().getAnnotation(javaClass<Ann>()).y
}

fun foo(ann: Ann, a: A) {
    ann.value
    bar(ann.x + (ann).y)

    a.foo()

    a.equals(a)
    a.toString()
    a.hashCode()

    class Local {
        val prop = ann.arg
        fun baz() {
            val v = ann.args
            val summ = ann.x * ann.ext()
        }

        fun Ann.ext(): Int  = -y
    }
}
