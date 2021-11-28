// WITH_STDLIB
// TARGET_BACKEND: JVM

// FILE: Foo.java
public interface Foo<R> {
    <E> String into(Class<? extends E> type);
    <H extends Bar<R>> String into(H handler);
}

// FILE: Bar.java
public class Bar<K> {}

// FILE: main.kt
inline fun <reified E: Any> Foo<*>.into(): String = into(E::class.java)

fun box(): String {
    return (object : Foo<Any> {
        override fun <E> into(type: Class<out E>?): String = "OK"
        override fun <H : Bar<Any>?> into(handler: H): String = "NOK"
    }).into<Int>()
}
