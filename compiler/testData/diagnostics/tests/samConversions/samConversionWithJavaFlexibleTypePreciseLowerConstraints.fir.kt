// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +PreciseSimplificationToFlexibleLowerConstraint
// FULL_JDK

// FILE: JavaBox.java
public class JavaBox<T> {
    public JavaBox(T b) {
        a = b;
    }
    public T a;
}

//FILE: Test.kt
import java.util.function.Supplier

fun test(){
    Supplier<String> {
        <!ARGUMENT_TYPE_MISMATCH("Nothing?; String")!>JavaBox(null).a<!>
    }

    val sam : Supplier<String> = Supplier {
        <!ARGUMENT_TYPE_MISMATCH!>JavaBox(null).a<!>
    }

    val sam2 = object : Supplier<String> {
        override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>get<!>() = JavaBox(null).a
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, flexibleType, functionDeclaration, javaFunction, javaType,
lambdaLiteral, localProperty, nullableType, override, propertyDeclaration */
