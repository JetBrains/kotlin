// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73434

// FILE: JavaHelper.java
public interface JavaHelper {
    static <E extends InterfaceC> InterfaceA<E> createInterfaceA() {
        return null;
    }
}

// FILE: main.kt
interface InterfaceB<E> {
    companion object {
        fun <E : Enum<E>> default() = object : InterfaceB<E> {}
    }
}

interface InterfaceA<T>
interface InterfaceC

fun <E : InterfaceC> createInterfaceAKotlin():InterfaceA<E> = TODO()

fun <E : Any> funA(
    intA: InterfaceA<E>,
    intB: InterfaceB<E>,
) {}

fun <E> funB() where E : Enum<E>, E : InterfaceC {
    <!TYPE_MISMATCH!>funA(
        JavaHelper.createInterfaceA(),
        InterfaceB.default(),
    )<!>

    funA<E>(
        JavaHelper.createInterfaceA(),
        InterfaceB.default(),
    )

    <!TYPE_MISMATCH!>funA(
        createInterfaceAKotlin(),
        InterfaceB.default(),
    )<!>

    funA<E>(
        createInterfaceAKotlin(),
        InterfaceB.default(),
    )
}
