// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89476
@file:OptIn(kotlin.ExperimentalStdlibApi::class)

@<!DEPRECATION!>EagerInitialization<!>
val topLevelVal = 1

@<!DEPRECATION!>EagerInitialization<!>
var topLevelVar = 1

class Person(val name: String) {
    <!INAPPLICABLE_EAGER_INITIALIZATION!>@<!DEPRECATION!>EagerInitialization<!><!>
    var surname: String? = null

    companion object {
        <!INAPPLICABLE_EAGER_INITIALIZATION!>@<!DEPRECATION!>EagerInitialization<!><!>
        val companionVal = 1
    }
}

object Singleton {
    <!INAPPLICABLE_EAGER_INITIALIZATION!>@<!DEPRECATION!>EagerInitialization<!><!>
    val objectVal = 1
}
