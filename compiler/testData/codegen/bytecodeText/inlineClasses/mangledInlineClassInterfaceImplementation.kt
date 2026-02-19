// LANGUAGE: +InlineClasses

interface A<T> {
    fun foo(a: T): String
}

inline class Foo(val x: Long) : A<Foo> {
    override fun foo(a: Foo): String = if (x != a.x) "OK" else "FAIL"
}

fun box(): String {
    return Foo(0L).foo(Foo(1L))
}

// 1 public foo-GWb7d6U\(J\)Ljava/lang/String;
// 1 public synthetic bridge foo\(Ljava/lang/Object;\)Ljava/lang/String;
// 1 public static foo-GWb7d6U\(JJ\)Ljava/lang/String;
// 0 foo\(J
