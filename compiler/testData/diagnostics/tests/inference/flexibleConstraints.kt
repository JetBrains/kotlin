// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// DUMP_INFERENCE_LOGS: MARKDOWN
// LATEST_LV_DIFFERENCE

// FILE: JavaClass.java
import org.jetbrains.annotations.*

public class JavaClass {
    public static <C> void consume(C c) {}
    public static <T> T transform(T t) { return null; }
    public static <T> @NotNull T transformNotNull(T t) { return null; }
    public static <T, U extends T> void consumeWithBounds(U u) {}
}

// FILE: Test.kt
fun test() {
    // String  <: C! => String! <: C
    JavaClass.consume("")
    val s: String? = null
    // String? <: C! => String? <: C
    JavaClass.consume(s)
    // String? <: T! => String? <: T, T! <: String => T <: String!
    eatString(<!TYPE_MISMATCH("String; String?")!>JavaClass.transform(s)<!>)
    // String? <: T! => String? <: T
    // We expect typeof(res) = String?, with String! res.toString() resolve will be changed (KT-81988)
    val res = JavaClass.transform(s)
    eatString(<!TYPE_MISMATCH("String; String?")!>res<!>)
}

fun eatString(s: String) {}

fun testTransformNotNull() {
    val s: String? = null
    // String? <: T!, T & Any <: String
    val res: String = JavaClass.transformNotNull(s)
}

fun <R> testWithTypeParameter() {
    val arg: R? = null
    // R? <: T!
    // We expect typeof(res) = R?
    val res = JavaClass.transform(arg)
}

fun testWithBounds() {
    val s: String? = null
    // U <: T, T = String!, String? <: U!
    JavaClass.consumeWithBounds<String, _>(s)
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, localProperty, nullableType, propertyDeclaration,
stringLiteral */
