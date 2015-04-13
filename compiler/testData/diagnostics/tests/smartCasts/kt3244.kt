// Should work already in M11

open class A(val s : String)
class B(s : String) : A(s)

fun test(a : A): String? {
    if (a is B) {
        // Earlier (14.01.2013) reported overload ambiguity over s
        return a.s 
    }
    return null
}