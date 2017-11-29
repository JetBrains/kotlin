// !WITH_NEW_INFERENCE
//If this test hangs, it means something is broken.
package a

object A {
    val iii = 42
}

//inappropriate but participating in resolve functions
fun foo(s: String, a: Any) = s + a
fun foo(a: Any, s: String) = s + a
fun foo(i: Int, j: Int) = i + j
fun foo(a: Any, i: Int) = "$a$i"
fun foo(f: (Int) -> Int, i: Int) = f(i)
fun foo(f: (String) -> Int, s: String) = f(s)
fun foo(f: (Any) -> Int, a: Any) = f(a)
fun foo(s: String, f: (String) -> Int) = f(s)
fun foo(a: Any, f: (Any) -> Int) = f(a)
//appropriate function
fun foo(i: Int, f: (Int) -> Int) = f(i)

fun <T> id(t: T) = t

fun test() {
    <!NI;OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(1, id(fun(x1: Int) =
          <!NI;OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(2, id(fun(x2: Int) =
                <!NI;OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(3, id(fun(x3: Int) =
                      <!NI;OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(4, id(fun(x4: Int) =
                            <!NI;OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(5, id(fun(x5: Int) =
                                  x1 + x2 + x3 + x4 + x5 + A.iii
                            ))
                      ))
                ))
          ))
    ))
}
