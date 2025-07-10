// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FIR_IDENTICAL
import kotlin.reflect.KClass

// Just some case found in IJ
fun <R, C : MutableList<R>> bar(c: C, x: () -> R): C = TODO()

fun <I> I.idExt(): I = TODO()

fun foo(x: Class<out Throwable>) {
    val a: MutableList<Class<out Throwable>> =
        bar(mutableListOf()) { x }.idExt()
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, functionDeclaration, lambdaLiteral, localProperty, outProjection,
primaryConstructor, propertyDeclaration, starProjection, typeConstraint, typeParameter */
