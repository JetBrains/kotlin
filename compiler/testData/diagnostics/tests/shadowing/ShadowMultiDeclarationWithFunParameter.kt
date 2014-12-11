// !DIAGNOSTICS: -UNUSED_PARAMETER
public inline fun <reified T> Array(n: Int, block: (Int) -> T): Array<T> = null!!

class A {
    fun component1() = 42
    fun component2() = 42
}

fun foo(a: A, c: Int) {
    val (<!NAME_SHADOWING!>a<!>, b) = a
    val arr = Array(2) { A() }
    for ((<!NAME_SHADOWING!>c<!>, d) in arr)  {
        
    }

    val <!UNUSED_VARIABLE!>e<!> = a.toString() + b + c
}