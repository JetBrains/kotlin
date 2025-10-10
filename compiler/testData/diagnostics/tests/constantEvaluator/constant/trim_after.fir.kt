// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB

const val trim1 = "123".trim()
const val trim2 = """ 123
    456
""".trim()


const val trimStart1 = "123".trimStart()
const val trimStart2 = """ 123 456 """.trimStart()

const val trimEnd1 = "123".trimEnd()
const val trimEnd2 = """ 123 456 """.trimEnd()

const val trimMargin1 = "123".trimMargin()
const val trimMargin2 = """ABC
                |123
                |456""".trimMargin()
const val trimMargin3 = """
    #XYZ
    #foo
    #bar
""".trimMargin("#")


const val trimIndent1 = "123".trimIndent()
const val trimIndent2 =
    """
            ABC
            123
            456
        """.trimIndent()

/* GENERATED_FIR_TAGS: additiveExpression, const, equalityExpression, integerLiteral, propertyDeclaration,
unsignedLiteral */
