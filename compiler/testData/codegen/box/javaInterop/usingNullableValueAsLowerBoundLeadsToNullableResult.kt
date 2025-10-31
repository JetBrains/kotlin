// TARGET_BACKEND: JVM
// WITH_STDLIB
// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-81988

// FILE: MyConsumer.java
public interface MyConsumer<T> {
    public void accept(T t);
}

// FILE: MyAssert.java
public class MyAssert<T> {
    public static <F> MyAssert<F> assertThat(java.util.List<? extends F> l) {
        return new MyAssert<F>();
    }

    public T getValue() { return null; }

    public void allSatisfy(MyConsumer<? super T> c) {
        c.accept(null);
    }
}

// FILE: main.kt

val String?.length: String get() = "OK"

fun box(): String {
    val x: List<String?> = listOf(null)
    var result = "fail"

    MyAssert.assertThat(x).allSatisfy { str ->
        val t = str.toString()
        result = t
    }

    if (result != "null") return "fail: $result"

    // Before the fix its type was Int
    val res2 = MyAssert.assertThat(x).getValue().length
    return "$res2"
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, getter, integerLiteral, javaFunction, nullableType,
propertyDeclaration, propertyWithExtensionReceiver, safeCall, stringLiteral */
