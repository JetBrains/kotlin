// IGNORE_BACKEND: JS, NATIVE
//  java.lang.AssertionError
//    at org.jetbrains.kotlin.js.translate.context.TranslationContext.getDispatchReceiver(TranslationContext.java:590)
//    at org.jetbrains.kotlin.js.translate.utils.TranslationUtils.backingFieldReference(TranslationUtils.java:237)
//    at org.jetbrains.kotlin.js.translate.utils.TranslationUtils.assignmentToBackingField(TranslationUtils.java:250)
//    at org.jetbrains.kotlin.js.translate.reference.BackingFieldAccessTranslator.translateAsSet(BackingFieldAccessTranslator.java:60)
//    ...

// FILE: kt49316.kt
import a.*

// This test should become irrelevant after KT-35565 is fixed.

fun test(foo: Foo): String {
    return foo.s

    // VAL_REASSIGNMENT not reported in unreachable code.
    // Make sure there's no BE internal error here.
    foo.s = "oops"
}

fun box() = test(Foo("OK"))

// FILE: Foo.kt
package a

class Foo(val s: String)
