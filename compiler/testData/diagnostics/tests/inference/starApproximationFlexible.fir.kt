// SKIP_TXT
// !LANGUAGE: +NewInference

// FILE: JavaClass.java
public class JavaClass {
    public static <T extends Self<?>> Self<T> id1Inv(T t) { return null; }
    public static <T extends Self<?>> T id2Inv(T t) { return null; }
    public static <T extends OutSelf<?>> OutSelf<T> id1Out(T t) { return null; }
    public static <T extends OutSelf<?>> T id2Out(T t) { return null; }
}

// FILE: main.kt
interface Self<E : Self<E>> {
    val x: E
}
fun bar(): Self<*> = TODO()

interface OutSelf<out E : OutSelf<E>> {
    val x: E
}
fun outBar(): OutSelf<*> = TODO()

fun main() {
    JavaClass.id1Inv(bar()).x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x
    JavaClass.id2Inv(bar()).x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x

    JavaClass.id1Out(outBar()).x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x
    JavaClass.id2Out(outBar()).x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x
}
