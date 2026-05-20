// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-...
// FULL_JDK
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

fun <T> foo(
    b: Boolean,
    handler: () -> T,
    execService: ExecutorService,
): Future<T> {
    return execService.submit(handler)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, funWithExtensionReceiver,
functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType, outProjection,
primaryConstructor, propertyDeclaration, stringLiteral, typeConstraint, typeParameter */
