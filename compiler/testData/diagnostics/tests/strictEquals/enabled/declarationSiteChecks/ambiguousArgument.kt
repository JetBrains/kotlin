// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: p/f1.kt
package p

interface A
interface B

// FILE: q/f2.kt
package q

interface A
interface B {
    companion object
}

// FILE: r/test.kt
package r

import p.*
import q.*

interface C : p.A, q.A {
    override fun equals(@EqualityBound(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!NONE_APPLICABLE!>A<!>::class<!>) other: Any?): Boolean
}

interface D : p.A {
    override fun equals(@EqualityBound(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!NONE_APPLICABLE!>A<!>::class<!>) other: Any?): Boolean
}

// See KT-88044
interface E : p.A {
    override fun equals(@EqualityBound(<!AMBIGUOUSLY_RESOLVED_EQUALITY_BOUND_ARGUMENT!>B<!>::class) other: Any?): Boolean
}

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, interfaceDeclaration, nullableType, operator, override */
