// RUN_PIPELINE_TILL: BACKEND
abstract class A<T : Any> {
    abstract protected fun T.foo()

    fun bar(x: T?) {
        if (x != null) {
            <!DEBUG_INFO_SMARTCAST!>x<!>.foo()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, equalityExpression, funWithExtensionReceiver, functionDeclaration,
ifExpression, nullableType, smartcast, typeConstraint, typeParameter */
