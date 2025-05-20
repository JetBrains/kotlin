// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73434

// FILE: JavaHelper.java
public interface JavaHelper {
    static <E1> InterfaceA<E1> createInterfaceA() {
        return null;
    }
}

// FILE: main.kt
interface InterfaceA<AT>
interface InterfaceB<BT>

fun <E1> createInterfaceAKotlin(): InterfaceA<E1> = TODO()
fun <E2 : Enum<E2>> defaultB(): InterfaceB<E2> = TODO()

fun <R> funA(
    intA: InterfaceA<R>,
    intB: InterfaceB<R>,
) {}

fun <E3 : Enum<E3>> funB() {
    <!TYPE_MISMATCH("CapturedType(*); Enum<*>")!>funA(
        createInterfaceAKotlin(),
        defaultB(),
    )<!>

    <!TYPE_MISMATCH("CapturedType(*)?; Enum<*>")!>funA(
        JavaHelper.createInterfaceA(),
        defaultB(),
    )<!>

    funA<E3>(
        createInterfaceAKotlin(),
        defaultB(),
    )

    funA<E3>(
        JavaHelper.createInterfaceA(),
        defaultB(),
    )
}
