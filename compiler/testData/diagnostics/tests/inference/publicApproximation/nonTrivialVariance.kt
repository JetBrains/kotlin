// DIAGNOSTICS: -UNUSED_PARAMETER

fun test(first: First, second: Second) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Inv<Bound1>>")!>test1(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<In<Bound1>>")!>test2(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Out<Bound1>>")!>test3(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("In<Inv<Bound1>>")!>test4(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("In<In<Bound1>>")!>test5(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("In<Out<Bound1>>")!>test6(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<Inv<Bound1>>")!>test7(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<In<Bound1>>")!>test8(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Bound1>>")!>test9(first, second)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<Out<Bound1>>>")!>test10(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Out<Out<Bound1>>>")!>test11(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Out<In<Bound1>>>")!>test12(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<In<Out<Bound1>>>")!>test13(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<In<In<Bound1>>>")!>test14(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<Inv<Out<Bound1>>>")!>test15(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<Out<In<Bound1>>>")!>test16(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<In<Out<Bound1>>>")!>test17(first, second)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("In<Out<Out<Bound1>>>")!>test18(first, second)<!>
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
