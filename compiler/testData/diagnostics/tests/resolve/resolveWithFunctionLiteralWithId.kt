//If this test hangs, it means something is broken.
object A {
    val iii = 42
}

//inappropriate but participating in resolve functions
fun foo(s: String, a: Any) = s + a
fun foo(a: Any) = a
fun foo(i: Int) = i
fun foo(a: Any, i: Int, f: ()-> Int) = "$a$i${f()}"
fun foo(f: (Int)->Int, i: Int) = f(i)
fun foo(f: (String)->Int, s: String) = f(s)
fun foo(f: (Any)->Int, a: Any) = f(a)
fun foo(s: String, f: (String, String)->Int) = f(s, s)
//appropriate function
fun foo(i: Int, f: (Int)->Int) = f(i)

fun <T> id(t: T) = t

fun test() {
    foo(1, id { x1 ->
        foo(2, id { x2 ->
            foo(3, id { x3 ->
                foo(4, id { x4 ->
                    foo(5, id { x5 ->
                        x1 + x2 + x3 + x4 + x5 + A.iii
                    })
                })
            })
        })
    })
}
