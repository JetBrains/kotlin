// RUN_PIPELINE_TILL: BACKEND

// MODULE: common
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>

// MODULE: intermediate()()(common)
actual class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!>

// MODULE: main()()(intermediate)
actual class <!AMBIGUOUS_EXPECTS!>A<!>
actual class <!AMBIGUOUS_EXPECTS!>C<!>
