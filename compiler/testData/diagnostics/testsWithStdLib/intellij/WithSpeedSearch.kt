// RUN_PIPELINE_TILL: BACKEND
class JList<E>

class ListSpeedSearch<T>(list: JList<T>)

class XThreadsFramesView {
    private fun <J> J.withSpeedSearch(): J where J : JList<*> {
        val search = ListSpeedSearch(this)
        return this
    }
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, funWithExtensionReceiver, functionDeclaration, localProperty,
nullableType, outProjection, primaryConstructor, propertyDeclaration, starProjection, thisExpression, typeConstraint,
typeParameter */
