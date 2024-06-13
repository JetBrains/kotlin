// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: Test.kt

fun test(arg: Derived) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Out<Base>")!>id<Out<Base>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("Out<(Derived..Derived?)>")!>createOut(
            <!DEBUG_INFO_EXPRESSION_TYPE("(Derived..Derived?)")!>JavaCls.makeFlexible(arg)<!>
        )<!>
    )<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("In<Base>")!>id<In<Base>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("In<(Base..Base?)>")!>createIn(
            <!DEBUG_INFO_EXPRESSION_TYPE("(Derived..Derived?)")!>JavaCls.makeFlexible(arg)<!>
        )<!>
    )<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Base>")!>id<Inv<Base>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<Base>")!>createInv(
            <!DEBUG_INFO_EXPRESSION_TYPE("(Derived..Derived?)")!>JavaCls.makeFlexible(arg)<!>
        )<!>
    )<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("JavaInv<Out<Base>>")!>id<JavaInv<Out<Base>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("JavaInv<Out<Base>>")!>createJavaInv(
            <!DEBUG_INFO_EXPRESSION_TYPE("(Derived..Derived?)")!>JavaCls.makeFlexible(arg)<!>
        )<!>
    )<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("In<In<Base>>")!>id<In<In<Base>>>(
        <!DEBUG_INFO_EXPRESSION_TYPE("In<In<(Derived..Derived?)>>")!>createInIn(
            <!DEBUG_INFO_EXPRESSION_TYPE("(Derived..Derived?)")!>JavaCls.makeFlexible(arg)<!>
        )<!>
    )<!>
}

interface Base
class Derived : Base

class Inv<T>
class Out<out O>
class In<in I>

fun <K> id(arg: K) = arg

fun <T> createInv(arg: T): Inv<T> = TODO()
fun <T> createOut(arg: T): Out<T> = TODO()
fun <T> createIn(arg: T): In<T> = TODO()
fun <T> createInIn(arg: T): In<In<T>> = TODO()
fun <T> createJavaInv(arg: T): JavaInv<Out<T>> = TODO()

// FILE: JavaCls.java

public class JavaCls {
    public static <T> T makeFlexible(T argument) {
        return argument;
    }
}

// FILE: JavaInv.java

public class JavaInv <T> {
}
