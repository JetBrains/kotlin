// LANGUAGE: +FullValueClasses
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

value class Point(val x: Int, val y: Int)

fun f(vararg points: Point) = points.asList().toString()

@JvmInline
value class OldPoint(val x: Int)

fun f(<!FORBIDDEN_VARARG_PARAMETER_TYPE!>vararg<!> points: OldPoint) = points.asList().toString()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, outProjection, primaryConstructor, propertyDeclaration,
value, vararg */
