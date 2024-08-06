// ImplicitArrayWithFlexibleParameterTypesKt
// SKIP_IDE_TEST
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

// FILE: ImplicitArrayWithFlexibleParameterTypes.kt

fun getArrayOfFlexibleInts() /* : Array<Int!> */ = arrayOf(JavaClass.getJavaInt())


// FILE: JavaClass.java

public class JavaClass {
    public static Integer getJavaInt() {
        return 10;
    }
}
