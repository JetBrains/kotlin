// !LANGUAGE: -DefaultImportOfPackageKotlinComparisons

fun foo() = compareBy<String> { it.length }
