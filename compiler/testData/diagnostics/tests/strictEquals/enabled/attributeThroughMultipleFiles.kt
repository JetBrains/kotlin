// RUN_PIPELINE_TILL: BACKEND
// SCOPE_DUMP: A:equals

// FILE: A.kt

class A : B.NestedB() {
    override fun equals(other: Any?): Boolean = other.i()
}

// FILE: B.kt

class B : C.NestedC() {
    open class NestedB : NestedMostC() {
        override fun equals(other: Any?): Boolean = true
    }
}

// FILE: C.kt

class C {
    open class NestedC {
        abstract class NestedMostC : I
    }
}

// FILE: I.kt

interface I {
    fun i() = true
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, nestedClass,
nullableType, operator, override, smartcast */
