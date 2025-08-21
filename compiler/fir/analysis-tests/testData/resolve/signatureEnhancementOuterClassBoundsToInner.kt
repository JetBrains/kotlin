// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-70327
// FILE: OuterClass.java
public abstract class OuterClass<T extends OuterClass<T>.InnerClass> {
    public class InnerClass {

    }
}

// FILE: main.kt
fun usage(o: OuterClass<*>.InnerClass) {

}

/* GENERATED_FIR_TAGS: functionDeclaration, javaType, starProjection */
