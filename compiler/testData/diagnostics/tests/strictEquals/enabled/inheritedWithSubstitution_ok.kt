// RUN_PIPELINE_TILL: BACKEND
// SCOPE_DUMP: StringBox:equals

interface Box<T> {
    override fun equals(@EqualityBound(Box::class) other: Any?): Boolean
}

abstract class AbstractBox<T> : Box<T> {
    override fun equals(@EqualityBound(Box::class) other: Any?): Boolean = true
}

class StringBox : AbstractBox<String>()

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, nullableType,
operator, override, typeParameter */
