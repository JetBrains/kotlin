// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82555

object Obj {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    object Nested

    val String.Nested get() = 42
}

@Deprecated("", level = DeprecationLevel.HIDDEN)
object TopLevel

val String.TopLevel get() = 42

fun test() {
    with("") {
        Obj.<!DEPRECATION_ERROR!>Nested<!>
        TopLevel
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, getter, integerLiteral, lambdaLiteral, nestedClass, objectDeclaration,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
