// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Box<out R>
fun <R> List<Box<R>>.choose(): Box<R>? = TODO()
fun list(): List<Box<*>> = TODO()

fun f() = list().choose()

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, interfaceDeclaration, nullableType, out,
starProjection, typeParameter */
