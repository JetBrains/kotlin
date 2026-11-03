// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// TARGET_BACKEND: JVM
// WITH_STDLIB

interface I

@JvmInline
value class InlineVal(val x: Int) : I

fun <T : <!FINAL_UPPER_BOUND!>InlineVal<!>> boundedByInlineValueClass(t: T) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>t<!>) {}
}

fun <T : <!FINAL_UPPER_BOUND!>Int<!>> boundedByPrimitive(t: T) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>t<!>) {}
}

fun <T> boundedByIntersection(t: T) where T : <!FINAL_UPPER_BOUND!>InlineVal<!>, T : I {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>t<!>) {}
}

fun direct(v: InlineVal, i: Int) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>v<!>) {}
    synchronized(<!SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE_ERROR!>i<!>) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, lambdaLiteral, primaryConstructor,
propertyDeclaration, typeConstraint, typeParameter, value */
