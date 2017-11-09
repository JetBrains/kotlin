// !LANGUAGE: -DefaultImportOfPackageKotlinComparisons

fun foo() = <!UNRESOLVED_REFERENCE!>compareBy<!><String> { <!UNRESOLVED_REFERENCE!>it<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>length<!> }
