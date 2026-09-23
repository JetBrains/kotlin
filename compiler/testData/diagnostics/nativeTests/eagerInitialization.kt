// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_FIR_DIAGNOSTICS
// ALLOW_KOTLIN_PACKAGE
// ISSUE: KT-89476
// FILE: annotation.kt
package kotlin.native

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class EagerInitialization

// FILE: test.kt
import kotlin.native.EagerInitialization

@EagerInitialization
val topLevelVal = 1

@EagerInitialization
var topLevelVar = 1

class Person(val name: String) {
    <!INAPPLICABLE_EAGER_INITIALIZATION!>@EagerInitialization<!>
    var surname: String? = null

    companion object {
        <!INAPPLICABLE_EAGER_INITIALIZATION!>@EagerInitialization<!>
        val companionVal = 1
    }
}

object Singleton {
    <!INAPPLICABLE_EAGER_INITIALIZATION!>@EagerInitialization<!>
    val objectVal = 1
}
