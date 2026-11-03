// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-88875
// WITH_STDLIB
// FULL_JDK
// MODULE: m1
const val REPLACEMENT = "bar()"

@Deprecated("Use 'bar' instead", ReplaceWith("bar()"))
fun withExpression() {}

@Deprecated("Use 'bar' instead.", ReplaceWith("Bar" + "()", "pkg.Bar"))
fun withSingleImport() {}

@Deprecated("Use 'bar' instead", ReplaceWith(expression = "Bar(Baz())", imports = ["pkg.Bar", "pkg.Baz"]))
fun withMultipleImports() {}

@Deprecated("Use 'bar' instead", ReplaceWith(imports = arrayOf("pkg.Bar"), expression = "Bar()"))
fun withArrayOfImports() {}

@Deprecated("Use 'bar' instead", ReplaceWith("Bar()", *["pkg.Bar"]))
fun withSpreadImports() {}

@Deprecated("Use 'bar' instead", ReplaceWith(REPLACEMENT))
fun withConstExpression() {}

// MODULE: m2(m1)
@Deprecated("", ReplaceWith("bar" + "()"))
fun withEmptyMessage() {}

@Deprecated("Use 'bar' instead", ReplaceWith(""))
fun withEmptyExpression() {}

@Deprecated("Use 'bar' instead", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("bar()"))
fun withError() {}

@Deprecated("Use 'newProp' instead", ReplaceWith("newProp"))
val oldProp = 1

@Deprecated("Use 'New' instead", ReplaceWith("New"))
class Old

typealias OldAlias = <!DEPRECATION!>Old<!>

fun bar() {}

fun test() {
    <!DEPRECATION!>withExpression<!>()
    <!DEPRECATION!>withSingleImport<!>()
    <!DEPRECATION!>withMultipleImports<!>()
    <!DEPRECATION!>withArrayOfImports<!>()
    <!DEPRECATION!>withSpreadImports<!>()
    <!DEPRECATION!>withConstExpression<!>()
    <!DEPRECATION!>withEmptyMessage<!>()
    <!DEPRECATION!>withEmptyExpression<!>()
    <!DEPRECATION_ERROR!>withError<!>()
    <!DEPRECATION!>oldProp<!>
    val x: <!DEPRECATION!>Old<!>? = null
    val y: <!TYPEALIAS_EXPANSION_DEPRECATION!>OldAlias<!>? = null

    // From a library
    "".<!DEPRECATION_ERROR!>toUpperCase<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, const, functionDeclaration, integerLiteral, localProperty,
nullableType, propertyDeclaration, stringLiteral, typeAliasDeclaration */
