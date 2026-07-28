// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-87439
// DUMP_INFERENCE_LOGS: MARKDOWN
// LANGUAGE: -RestrictSecondKindIncorporationToFixation

// FILE: Box.java
public class Box<A> {
    public static <A1> Box<A1> create1(A1 value) {
        return null;
    }

    public static <A2> Box<A2> create2(A2 value) {
        return null;
    }
}

// FILE: main.kt

fun show(s: String?): Box<Box<String>> {
    return Box.create1(Box.create2(s))
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, nullableType */
