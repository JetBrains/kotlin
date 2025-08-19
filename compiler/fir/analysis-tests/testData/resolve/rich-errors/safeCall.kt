// RUN_PIPELINE_TILL: BACKEND

error object E1

//val nString: String? = null!!
//val eString: String | E1 = null!!
//val neString: String? | E1 = null!!
//
//fun String.foo(): Int = null!!
//
//fun <T : Any? | E1> identity(v: T): T = v
//
//val v1: Int? = nString?.foo()
//val v2: Int | E1 = eString?.foo()
//val v3: Int? | E1 = neString?.foo()
//val v4: Int = <!INITIALIZER_TYPE_MISMATCH!>eString?.foo()<!>
//val v5: Int? = <!INITIALIZER_TYPE_MISMATCH!>neString?.foo()<!>
//val v12: Int? = identity(nString?.foo())
//val v22: Int | E1 = identity(eString?.foo())
//val v32: Int? | E1 = identity(neString?.foo())
//val v42: Int = <!INITIALIZER_TYPE_MISMATCH!>identity(eString?.foo())<!>
//val v52: Int? = <!INITIALIZER_TYPE_MISMATCH!>identity(neString?.foo())<!>

fun foo(v: Int | E1): Int {
    val tmp = v?.let { return it }
    val tmp2: E1 = tmp
    null!!
}
