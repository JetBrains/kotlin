// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
fun interface JavaVersionRestriction {

    fun isRestricted(gradleVersion: Int, source: Int): Boolean

    companion object {
        @JvmField
        val NO = JavaVersionRestriction { _, _ -> false }

        @JvmField
        val DEFAULT = compositeOf(listOf(
            JavaVersionRestriction { gradleVersion, _ -> gradleVersion < 7 }
        ))

        @JvmStatic
        fun compositeOf(restrictions: List<JavaVersionRestriction>): JavaVersionRestriction {
            return JavaVersionRestriction { gradleVersion, source ->
                restrictions.any { it.isRestricted(gradleVersion, source) }
            }
        }
    }
}

/* GENERATED_FIR_TAGS: companionObject, comparisonExpression, funInterface, functionDeclaration, integerLiteral,
interfaceDeclaration, lambdaLiteral, objectDeclaration, propertyDeclaration */
