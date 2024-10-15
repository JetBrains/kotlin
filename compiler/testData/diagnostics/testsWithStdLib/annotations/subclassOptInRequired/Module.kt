// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// MODULE: a
package a

@RequiresOptIn
annotation class Boom

@SubclassOptInRequired(Boom::class)
open class B {}

// MODULE: b(a)
package b
import a.B

class C : <!OPT_IN_TO_INHERITANCE_ERROR!>B<!>()
