// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

open class Simple {
    override fun equals(@EqualityBound(bound = Simple::class) other: Any?): Boolean = true
}

class SimpleChild : Simple() {
    override fun equals(@EqualityBound(Simple::class) other: Any?): Boolean = true
}

class FullName : Simple() {
    override fun equals(@kotlin.EqualityBound(Simple::class) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, operator, override */
