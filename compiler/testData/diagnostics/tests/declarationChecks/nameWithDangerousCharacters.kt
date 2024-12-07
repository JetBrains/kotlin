// RUN_PIPELINE_TILL: FIR2IR
// DISABLE_NEXT_TIER_SUGGESTION: D8 dexing error: com.android.tools.r8.errors.b: Class descriptor 'LA?B;' cannot be represented in dex format.
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
