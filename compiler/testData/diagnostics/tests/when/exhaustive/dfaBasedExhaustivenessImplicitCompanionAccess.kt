// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-86905

sealed class SealedWithCompanion {
    object A : SealedWithCompanion()
    companion object : SealedWithCompanion()
}

fun test1(s: SealedWithCompanion) {
    if (s != SealedWithCompanion) return
    when (s) {
        SealedWithCompanion -> 1
    }
}

fun test2(s: SealedWithCompanion) {
    if (s != SealedWithCompanion.Companion) return
    when (s) {
        SealedWithCompanion -> 1
    }
}

fun test3(s: SealedWithCompanion) {
    if (s != SealedWithCompanion) return
    when (s) {
        SealedWithCompanion.Companion -> 1
    }
}

fun test4(s: SealedWithCompanion) {
    if (s != SealedWithCompanion.Companion) return
    when (s) {
        SealedWithCompanion.Companion -> 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, equalityExpression, functionDeclaration, ifExpression,
integerLiteral, nestedClass, objectDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
