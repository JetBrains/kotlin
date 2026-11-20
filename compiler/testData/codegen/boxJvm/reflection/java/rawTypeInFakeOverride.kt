// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: A.java
public class A<T> {
    public A raw;
}

// FILE: B.java
public class B<T> extends A<T> {}

// FILE: C.java
public class C<T> extends A {}

// FILE: box.kt
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(A::class.java, A<*>::raw.returnType.javaType)
    assertEquals(A::class.java, B<*>::raw.returnType.javaType)
    assertEquals(A::class.java, C<*>::raw.returnType.javaType)
    return "OK"
}
