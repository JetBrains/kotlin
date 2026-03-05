// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// FIR_IDENTICAL
package foo

val Int.<!REDECLARATION!>simple<!>: Int get() = 0
val Int.<!REDECLARATION!>simple<!>: Int get() = 0

val Int.differentTypes: Int get() = 0
val String.differentTypes: Int get() = 0

val <T> T.<!REDECLARATION!>sameNamedGenerics<!>: Int get() = 0
val <T> T.<!REDECLARATION!>sameNamedGenerics<!>: Int get() = 0

val <T> T.<!REDECLARATION!>differentlyNamedGenerics<!>: Int get() = 0
val <R> R.<!REDECLARATION!>differentlyNamedGenerics<!>: Int get() = 0

val <T> T.sameNamedGenericsDifferentBounds: Int get() = 0
val <T : Any> T.sameNamedGenericsDifferentBounds: String get() = ""

val <T> T.differentlyNamedGenericsDifferentBounds: Int get() = 0
val <R : Any> R.differentlyNamedGenericsDifferentBounds: String get() = ""

val <T> List<T>.sameNamedGenericsListDifferentBounds: Int get() = 0
val <T : Any> List<T>.sameNamedGenericsListDifferentBounds: String get() = ""

val <T> List<T>.differentlyNamedGenericsListDifferentBounds: Int get() = 0
val <R : Any> List<R>.differentlyNamedGenericsListDifferentBounds: String get() = ""

context(_: Int) val <!REDECLARATION!>simpleContextReceivers<!>: Int get() = 0
context(_: Int) val <!REDECLARATION!>simpleContextReceivers<!>: Int get() = 0

context(_: Int) val differentTypesContextReceivers: Int get() = 0
context(_: String) val differentTypesContextReceivers: Int get() = 0

context(_: T) val <T> <!REDECLARATION!>sameNamedGenericsContextReceivers<!>: Int get() = 0
context(_: T) val <T> <!REDECLARATION!>sameNamedGenericsContextReceivers<!>: Int get() = 0

context(_: T) val <T> <!REDECLARATION!>differentlyNamedGenericsContextReceivers<!>: Int get() = 0
context(_: R) val <R> <!REDECLARATION!>differentlyNamedGenericsContextReceivers<!>: Int get() = 0

context(_: T) val <T> sameNamedGenericsDifferentBoundsContextReceivers: Int get() = 0
context(_: T) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <T : Any> sameNamedGenericsDifferentBoundsContextReceivers: String<!> get() = ""

context(_: T) val <T> differentlyNamedGenericsDifferentBoundsContextReceivers: Int get() = 0
context(_: R) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <R : Any> differentlyNamedGenericsDifferentBoundsContextReceivers: String<!> get() = ""

context(_: List<T>) val <T> sameNamedGenericsListDifferentBoundsContextReceivers: Int get() = 0
context(_: List<T>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <T : Any> sameNamedGenericsListDifferentBoundsContextReceivers: String<!> get() = ""

context(_: List<T>) val <T> differentlyNamedGenericsListDifferentBoundsContextReceivers: Int get() = 0
context(_: List<R>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <R : Any> differentlyNamedGenericsListDifferentBoundsContextReceivers: String<!> get() = ""

val Int.extensionVsContextReceiver: Int get() = 0
context(_: Int) val extensionVsContextReceiver: String get() = ""

class C {
    val Int.<!REDECLARATION!>simple<!>: Int get() = 0
    val Int.<!REDECLARATION!>simple<!>: Int get() = 0

    val Int.differentTypes: Int get() = 0
    val String.differentTypes: Int get() = 0

    val <T> T.<!REDECLARATION!>sameNamedGenerics<!>: Int get() = 0
    val <T> T.<!REDECLARATION!>sameNamedGenerics<!>: Int get() = 0

    val <T> T.<!REDECLARATION!>differentlyNamedGenerics<!>: Int get() = 0
    val <R> R.<!REDECLARATION!>differentlyNamedGenerics<!>: Int get() = 0

    val <T> T.sameNamedGenericsDifferentBounds: Int get() = 0
    val <T : Any> T.sameNamedGenericsDifferentBounds: String get() = ""

    val <T> T.differentlyNamedGenericsDifferentBounds: Int get() = 0
    val <R : Any> R.differentlyNamedGenericsDifferentBounds: String get() = ""

    val <T> List<T>.sameNamedGenericsListDifferentBounds: Int get() = 0
    val <T : Any> List<T>.sameNamedGenericsListDifferentBounds: String get() = ""

    val <T> List<T>.differentlyNamedGenericsListDifferentBounds: Int get() = 0
    val <R : Any> List<R>.differentlyNamedGenericsListDifferentBounds: String get() = ""

    context(_: Int) val <!REDECLARATION!>simpleContextReceivers<!>: Int get() = 0
    context(_: Int) val <!REDECLARATION!>simpleContextReceivers<!>: Int get() = 0

    context(_: Int) val differentTypesContextReceivers: Int get() = 0
    context(_: String) val differentTypesContextReceivers: Int get() = 0

    context(_: T) val <T> <!REDECLARATION!>sameNamedGenericsContextReceivers<!>: Int get() = 0
    context(_: T) val <T> <!REDECLARATION!>sameNamedGenericsContextReceivers<!>: Int get() = 0

    context(_: T) val <T> <!REDECLARATION!>differentlyNamedGenericsContextReceivers<!>: Int get() = 0
    context(_: R) val <R> <!REDECLARATION!>differentlyNamedGenericsContextReceivers<!>: Int get() = 0

    context(_: T) val <T> sameNamedGenericsDifferentBoundsContextReceivers: Int get() = 0
    context(_: T) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <T : Any> sameNamedGenericsDifferentBoundsContextReceivers: String<!> get() = ""

    context(_: T) val <T> differentlyNamedGenericsDifferentBoundsContextReceivers: Int get() = 0
    context(_: R) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <R : Any> differentlyNamedGenericsDifferentBoundsContextReceivers: String<!> get() = ""

    context(_: List<T>) val <T> sameNamedGenericsListDifferentBoundsContextReceivers: Int get() = 0
    context(_: List<T>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <T : Any> sameNamedGenericsListDifferentBoundsContextReceivers: String<!> get() = ""

    context(_: List<T>) val <T> differentlyNamedGenericsListDifferentBoundsContextReceivers: Int get() = 0
    context(_: List<R>) <!CONTEXTUAL_OVERLOAD_SHADOWED!>val <R : Any> differentlyNamedGenericsListDifferentBoundsContextReceivers: String<!> get() = ""

    val Int.extensionVsContextReceiver: Int get() = 0
    context(_: Int) val extensionVsContextReceiver: String get() = ""
}

/* GENERATED_FIR_TAGS: classDeclaration, getter, integerLiteral, nullableType, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver, stringLiteral, typeConstraint, typeParameter */
