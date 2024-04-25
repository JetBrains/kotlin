// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: Test.kt

fun test(arg: Derived) {
    id<Out<Base>>(
        createOut(
            JavaCls.makeFlexible(arg)
        )
    )
    id<In<Base>>(
        createIn(
            JavaCls.makeFlexible(arg)
        )
    )
    id<Inv<Base>>(
        createInv(
            JavaCls.makeFlexible(arg)
        )
    )
    id<JavaInv<Out<Base>>>(
        createJavaInv(
            JavaCls.makeFlexible(arg)
        )
    )
    id<In<In<Base>>>(
        createInIn(
            JavaCls.makeFlexible(arg)
        )
    )
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
