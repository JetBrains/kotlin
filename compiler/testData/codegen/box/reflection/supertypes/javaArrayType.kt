// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/A.java
package test;

public class A<T> {}

// FILE: test/B.java
package test;

import java.util.List;

public class B {
    public static class Test1 extends A<String[]> {}
    public static class Test2 extends A<List<String>[]> {}
    public static class Test3 extends A<Comparable<Number>[][]> {}

    public static String[] expectedType1() { return null; }
    public static Class<?> expectedClassifier1() { return String[].class; }
    public static List<String>[] expectedType2() { return null; }
    public static Class<?> expectedClassifier2() { return List[].class; }
    public static Comparable<Number>[][] expectedType3() { return null; }
    public static Class<?> expectedClassifier3() { return Comparable[][].class; }
}

// FILE: box.kt
import kotlin.test.assertEquals
import test.*

fun box(): String {
    val actual1 = B.Test1::class.supertypes.single().arguments.single().type!!
    assertEquals(B::expectedType1.returnType, actual1)
    assertEquals(B.expectedClassifier1().kotlin, actual1.classifier)

    val actual2 = B.Test2::class.supertypes.single().arguments.single().type!!
    assertEquals(B::expectedType2.returnType, actual2)
    assertEquals(B.expectedClassifier2().kotlin, actual2.classifier)

    val actual3 = B.Test3::class.supertypes.single().arguments.single().type!!
    assertEquals(B::expectedType3.returnType, actual3)
    assertEquals(B.expectedClassifier3().kotlin, actual3.classifier)

    return "OK"
}
