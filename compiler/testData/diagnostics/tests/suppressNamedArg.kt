// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-62146

@Deprecated("This is deprecated", level = DeprecationLevel.WARNING)
fun deprecated() = 1

@Suppress("DEPRECATION")
fun main() = deprecated()

@Suppress(names = ["DEPRECATION"])
fun plain() = deprecated()

@Suppress(names = ["DEPR" + "ECATION"])
fun sum() = deprecated()

@Suppress(names = arrayOf("DEPRECATION"))
fun brain() = deprecated()

@Suppress(names = arrayOf(*["DEPRECATION"]))
fun nested() = deprecated()

@Suppress(names = arrayOf(elements = ["DEPRE" + "CATION"]))
fun nestedNamed() = deprecated()

@Suppress(*[], "DEPRECATION")
fun firstEmptySpread() = deprecated()

@Suppress(*["DEPRECATION_ERROR"], "DEPRECATION")
fun firstNonEmptySpread() = deprecated()

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, stringLiteral */
