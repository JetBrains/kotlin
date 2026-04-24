// RUN_PIPELINE_TILL: BACKEND
// FILE: JavaWrapper.java

public class JavaWrapper<T> {
    public JavaWrapper(T arg) {}
}

// FILE: test.kt

fun <Y> foo() {
    val r = JavaWrapper<Y>(null)
    val s = JavaWrapper<String>(null)
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, localProperty, nullableType, propertyDeclaration,
typeParameter */
