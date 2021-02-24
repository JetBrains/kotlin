// !LANGUAGE: -DefaultImportOfPackageKotlinComparisons

fun foo() = <!UNRESOLVED_REFERENCE!>compareBy<!><String> { <!UNRESOLVED_REFERENCE!>it<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>length<!> }
