package foo

class A(val i: Int?) {
    fun test1() {
        if (this@A.i != null) {
            useInt(<!DEBUG_INFO_SMARTCAST!>this.i<!>)
            useInt(<!DEBUG_INFO_SMARTCAST!>i<!>)
        }
    }

    inner class B {
        fun test2() {
            if (i != null) {
                useInt(<!DEBUG_INFO_SMARTCAST!>this@A.i<!>)
            }
        }
    }
}

fun A.foo() {
    if (this@foo.i != null) {
        useInt(<!DEBUG_INFO_SMARTCAST!>this.i<!>)
        useInt(<!DEBUG_INFO_SMARTCAST!>i<!>)
    }
}

fun test3() {
    useFunction {
        if(i != null) {
            useInt(<!DEBUG_INFO_SMARTCAST!>this.i<!>)
        }
    }
}

fun useInt(i: Int) = i
fun useFunction(f: A.() -> Unit) = f