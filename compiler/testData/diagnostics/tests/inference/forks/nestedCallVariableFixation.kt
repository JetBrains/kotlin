// RUN_PIPELINE_TILL: BACKEND
// SKIP_TXT
// FIR_IDENTICAL
// FULL_JDK
// WITH_STDLIB
// FILE: PyTokenTypes.java
public class PyTokenTypes {
    public static final PyTokenTypes LT = new PyTokenTypes();
}

// FILE: main.kt
private val comparisonStrings = hashMapOf(
    PyTokenTypes.LT to "<",
)

fun findComparisonNegationOperators(x: PyTokenTypes?): Pair<String, String>? {
    return comparisonStrings.getValue(x) to
            comparisonStrings.getValue(x)
}
