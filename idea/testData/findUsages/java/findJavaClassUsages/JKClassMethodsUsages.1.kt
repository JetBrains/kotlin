public class X(bar: String? = A.BAR): A() {
    var next: A? = A()
    val myBar: String? = A.BAR

    {
        A.BAR = ""
        A.foos()
    }

    fun foo(a: A) {
        val aa: A = a
        aa.bar = ""
    }

    fun getNext(): A? {
        return next
    }

    public override fun foo() {
        super<A>.foo()
    }

    default object: A() {

    }
}

object O: A() {

}

fun X.bar(a: A = A()) {

}

fun Any.toA(): A? {
    return if (this is A) this as A else null
}