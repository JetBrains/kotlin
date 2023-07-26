// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses

// FILE: A.java
public @interface A {
    String value() default "OK";
}

// FILE: ChildAnnotation.java
@interface ChildAnnotation {
    String value();
}

// FILE: D.java
public @interface D {
    //Primitive types
    boolean booleanValue() default false;
    byte byteValue() default 1;
    short shortValue() default 2;
    int intValue() default 3;
    long longValue() default 4;
    float floatValue() default 5.0f;
    double doubleValue() default 6.0;
    char charValue() default 'a';

    // String
    String stringValue() default "default";

    // Class type
    Class<?> classValue() default Object.class;

    // Enum type
    MyEnum enumValue() default MyEnum.FIRST;

    // Annotation
    ChildAnnotation annotationValue() default @ChildAnnotation(value = "child") ;

    // Arrays of the above
    boolean[] booleanArrayValue() default { false, true };
    byte[] byteArrayValue() default { 1, 2 };
    short[] shortArrayValue() default { 3, 4 };
    int[] intArrayValue() default { 5, 6 };
    long[] longArrayValue() default { 7, 8 };
    float[] floatArrayValue() default { 9.0f, 10.0f };
    double[] doubleArrayValue() default { 11.0, 12.0 };
    char[] charArrayValue() default { 'x', 'y' };
    String[] stringArrayValue() default { "Hello", "World" };
    Class<?>[] classArrayValue() default { Object.class, String.class };
    MyEnum[] enumArrayValue() default { MyEnum.FIRST, MyEnum.SECOND };
    ChildAnnotation[] annotationArrayValue() default { @ChildAnnotation(value = "child1"), @ChildAnnotation(value = "child2") };

    // other annotation with default value
    A annotationWithDefault() default @A;
}

// Supporting enum
enum MyEnum { FIRST, SECOND }

// FILE: b.kt

fun box(): String {
    val d = D()
    val str = d.toString()
    val golden = """@D(booleanValue=false, byteValue=1, shortValue=2, intValue=3, longValue=4, floatValue=5.0, doubleValue=6.0, charValue=a, stringValue=default, """+
       """classValue=class java.lang.Object, enumValue=FIRST, annotationValue=@ChildAnnotation(value=child), booleanArrayValue=[false, true], byteArrayValue=[1, 2], """ +
       """shortArrayValue=[3, 4], intArrayValue=[5, 6], longArrayValue=[7, 8], floatArrayValue=[9.0, 10.0], doubleArrayValue=[11.0, 12.0], charArrayValue=[x, y], stringArrayValue=[Hello, World], """ +
       """classArrayValue=[class java.lang.Object, class java.lang.String], enumArrayValue=[FIRST, SECOND], annotationArrayValue=[@ChildAnnotation(value=child1), @ChildAnnotation(value=child2)], """ +
       """annotationWithDefault=@A(value=OK))"""
    if (str != golden) return str
    if (d.longValue != 4L) return d.longValue.toString()
    if (d.annotationValue.value != "child") return d.annotationValue.value
    if (d.doubleArrayValue[0] != 11.0) return d.doubleArrayValue[0].toString()
    if (d.classArrayValue[1] != String::class) return d.classArrayValue.contentToString()
    return d.annotationWithDefault.value
}
