package a

//KT-2234 'period!!' has type Int?

fun main(args : Array<String>) {
    val d : Long = 1
    val period : Int? = null
    if (period != null) #(d, period<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> : Int) else #(d, 1)
    if (period != null) #(d, period : Int) else #(d, 1)
}

fun foo() {
    val x : Int? = 3
    if (x != null)  {
        val <!UNUSED_VARIABLE!>u<!> = x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!> : Int
        val y = x : Int
        val <!UNUSED_VARIABLE!>z<!> : Int = y
    }
}
