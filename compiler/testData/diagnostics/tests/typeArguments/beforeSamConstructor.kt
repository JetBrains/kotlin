// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE_FEATURE_TOGGLED: ForbidUselessTypeArgumentsIn25

// FILE: J.java

public class J {
    public interface I {
        void foo();
    }
}

// FILE: main.kt

class K<TP> {
    fun interface I {
        fun foo()
    }
}

fun test() {
    K.I { }
    J.I { }
    K<!TYPE_ARGUMENTS_NOT_ALLOWED!><Int><!>.I { }
    J<!TYPE_ARGUMENTS_NOT_ALLOWED!><Int><!>.I { }
    K<!TYPE_ARGUMENTS_NOT_ALLOWED!><*, *><!>.I { }
    J<!TYPE_ARGUMENTS_NOT_ALLOWED!><*, *><!>.I { }
}

/* GENERATED_FIR_TAGS: classDeclaration, funInterface, functionDeclaration, interfaceDeclaration, javaType,
lambdaLiteral, nestedClass, nullableType, typeParameter */
