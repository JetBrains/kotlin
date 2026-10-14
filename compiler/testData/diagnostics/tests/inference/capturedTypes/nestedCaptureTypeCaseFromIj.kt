// RUN_PIPELINE_TILL: FRONTEND
import kotlin.reflect.KClass

// Just some case found in IJ
class ExceptionDescriptor<T : Throwable>(
    val type: KClass<out T>,
)

fun <R, C : MutableList<R>> bar(x: (ExceptionDescriptor<*>) -> R): MutableList<R> = TODO()

fun foo(x: List<ExceptionDescriptor<*>>) {
    val a: MutableList<Class<out Throwable>> =
        x.mapTo(mutableListOf()) { it.type.java }.<!INITIALIZER_TYPE_MISMATCH!>also<!> { y ->}
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, functionDeclaration, lambdaLiteral, localProperty, outProjection,
primaryConstructor, propertyDeclaration, starProjection, typeConstraint, typeParameter */
