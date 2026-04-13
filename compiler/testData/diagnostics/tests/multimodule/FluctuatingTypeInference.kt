// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP
// LATEST_LV_DIFFERENCE
// ISSUE: KT-80093

// MODULE: base
// FILE: base.kt
abstract class Base

// MODULE: missing(base)
// FILE: missing.kt
open class Missing : Base()

// MODULE: middle(base, missing)
// FILE: middle.kt
class Middle : Missing()

// MODULE: foo(base, middle)
// FILE: foo.kt
fun foo(flag: Boolean) {
    // Inferred to Any. Adding 'missing' dependency changes inferred type to 'Base'
    val myParent =
        if (flag) {
            BaseImpl()
        } else {
            <!MISSING_DEPENDENCY_SUPERCLASS_WARNING!>Middle<!>()
        }
    val myParent2: Base <!INITIALIZER_TYPE_MISMATCH!>=<!>
        if (flag) {
            BaseImpl()
        } else {
            <!MISSING_DEPENDENCY_SUPERCLASS_WARNING!>Middle<!>()
        }
}
class BaseImpl : Base()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, localProperty, propertyDeclaration */
