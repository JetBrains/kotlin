// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT
import java.util.*

val a = ArrayList<String>()

fun foo(l: List<String>) {
    a.plusAssign(l)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, propertyDeclaration */
