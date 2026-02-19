// RUN_PIPELINE_TILL: BACKEND
// IGNORE_DEXING
// FIR_IDENTICAL
class <!DANGEROUS_CHARACTERS!>`A?B`<!>
class <!DANGEROUS_CHARACTERS!>`A*B`<!>
class <!DANGEROUS_CHARACTERS!>`A"B`<!>
class <!DANGEROUS_CHARACTERS!>`A|B`<!>
class <!DANGEROUS_CHARACTERS!>`A%B`<!>

fun <!DANGEROUS_CHARACTERS!>`?*"|%`<!>(): Int {
    val <!DANGEROUS_CHARACTERS!>`?`<!> = 0
    return `?`
}

val <!DANGEROUS_CHARACTERS!>`"a"+"b"`<!> = "c"

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
stringLiteral */
