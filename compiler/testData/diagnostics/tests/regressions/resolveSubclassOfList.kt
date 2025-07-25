// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
import java.util.ArrayList

fun foo(p: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.util.List<String><!>) {
    p.iterator(); // forcing resolve of java.util.List.iterator()

    ArrayList<String>().iterator(); // this provoked exception in SignaturesPropagationData
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction */
