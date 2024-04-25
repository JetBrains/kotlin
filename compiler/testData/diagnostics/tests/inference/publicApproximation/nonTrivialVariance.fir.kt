// DIAGNOSTICS: -UNUSED_PARAMETER

fun test(first: First, second: Second) {
    test1(first, second)
    test2(first, second)
    test3(first, second)
    test4(first, second)
    test5(first, second)
    test6(first, second)
    test7(first, second)
    test8(first, second)
    test9(first, second)

    test10(first, second)
    test11(first, second)
    test12(first, second)
    test13(first, second)
    test14(first, second)
    test15(first, second)
    test16(first, second)
    test17(first, second)
    test18(first, second)
}

interface Bound1
interface Bound2
object First : Bound1, Bound2
object Second : Bound1, Bound2

fun <S : Bound1> intersect(vararg elements: S): S = TODO()

class Inv<T>(val prop: T)
class In<in I>(arg: I)
class Out<out O>(val arg: O)

fun test1(first: First, second: Second) = Inv(Inv(intersect(first, second)))
fun test2(first: First, second: Second) = Inv(In(intersect(first, second)))
fun test3(first: First, second: Second) = Inv(Out(intersect(first, second)))
fun test4(first: First, second: Second) = In(Inv(intersect(first, second)))
fun test5(first: First, second: Second) = In(In(intersect(first, second)))
fun test6(first: First, second: Second) = In(Out(intersect(first, second)))
fun test7(first: First, second: Second) = Out(Inv(intersect(first, second)))
fun test8(first: First, second: Second) = Out(In(intersect(first, second)))
fun test9(first: First, second: Second) = Out(Out(intersect(first, second)))

fun test10(first: First, second: Second) = Out(Out(Out(intersect(first, second))))
fun test11(first: First, second: Second) = Inv(Out(Out(intersect(first, second))))
fun test12(first: First, second: Second) = Inv(Out(In(intersect(first, second))))
fun test13(first: First, second: Second) = Inv(In(Out(intersect(first, second))))
fun test14(first: First, second: Second) = Inv(In(In(intersect(first, second))))
fun test15(first: First, second: Second) = Out(Inv(Out(intersect(first, second))))
fun test16(first: First, second: Second) = Out(Out(In(intersect(first, second))))
fun test17(first: First, second: Second) = Out(In(Out(intersect(first, second))))
fun test18(first: First, second: Second) = In(Out(Out(intersect(first, second))))
