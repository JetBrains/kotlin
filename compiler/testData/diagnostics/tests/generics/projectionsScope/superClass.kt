// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

interface Clazz<T> {
    val t: T
    fun getSuperClass(): Clazz<in T>
}

fun test(clazz: Clazz<*>) {
    clazz.t checkType { _<Any?>() }
    clazz.getSuperClass() checkType { _<Clazz<*>>() }
    clazz.getSuperClass().t checkType { _<Any?>() }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
inProjection, infix, interfaceDeclaration, lambdaLiteral, nullableType, outProjection, propertyDeclaration,
starProjection, typeParameter, typeWithExtension */
