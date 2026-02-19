// RUN_PIPELINE_TILL: BACKEND
class C<T>(var x: T)

var <T> C<T>.y
    get() = x
    set(v) {
        x = v
    }

fun use(f: () -> String) {}

fun test1() {
    use { C("abc").y }
    use(C("abc")::y)
}

/* GENERATED_FIR_TAGS: assignment, callableReference, classDeclaration, functionDeclaration, functionalType, getter,
lambdaLiteral, nullableType, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver, setter,
stringLiteral, typeParameter */
