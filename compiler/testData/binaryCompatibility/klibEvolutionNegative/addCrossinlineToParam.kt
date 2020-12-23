// MODULE: lib
// FILE: A.kt
// VERSION: 1

inline fun foo(x: () -> Unit) = x()

// FILE: B.kt
// VERSION: 2

fun regular(y: () -> Unit) = y()

inline fun foo(crossinline x: () -> Unit) = regular{ x() }

// MODULE: mainLib(lib)
// FILE: mainLib.kt

val list = mutableListOf('p', 'a', 'j', 'a', 'm', 'a', 's')

fun lib(): String {
    foo { 
        list.reverse()
        return "UNEXPECTED INLINE"
    } 

    return "UNEXPECTED RETURN"
}

// MODULE: main(mainLib)
// FILE: main.kt
fun box(): String = lib()

