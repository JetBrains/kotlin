package foo

class A(val i: Int?) {
    fun test1() {
        if (this@A.i != null) {
            useInt(this.i)
            useInt(i)
        }
    }

    inner class B {
        fun test2() {
            if (i != null) {
                useInt(this@A.i)
            }
        }
    }
}

fun A.foo() {
    if (this@foo.i != null) {
        useInt(this.i)
        useInt(i)
    }
}

fun test3() {
    useFunction {
        if(i != null) {
            useInt(this.i)
        }
    }
}

fun useInt(i: Int) = i
fun useFunction(f: A.() -> Unit) = f