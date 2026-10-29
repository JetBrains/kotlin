// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

abstract class MyAbstractWithCtor constructor()

fun main() {
    val appendable = <!NO_COMPANION_OBJECT!>Appendable<!>
    val stringBuilder = <!NO_COMPANION_OBJECT!>StringBuilder<!>
    val exception = <!NO_COMPANION_OBJECT!>IllegalStateException<!>
    val array = <!NO_COMPANION_OBJECT!>Array<Int><!>
    val retention = kotlin.annotation.<!NO_COMPANION_OBJECT!>Retention<!>
    val myAbstractWithCtor = <!NO_COMPANION_OBJECT!>MyAbstractWithCtor<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, propertyDeclaration */
