// !LANGUAGE: +ContextReceivers
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

context(Int) val <!REDECLARATION!>simpleContextReceivers<!>: Int get() = 0
context(Int) val <!REDECLARATION!>simpleContextReceivers<!>: Int get() = 0

context(Int) val differentTypesContextReceivers: Int get() = 0
context(String) val differentTypesContextReceivers: Int get() = 0

context(T) val <T> <!REDECLARATION!>sameNamedGenericsContextReceivers<!>: Int get() = 0
context(T) val <T> <!REDECLARATION!>sameNamedGenericsContextReceivers<!>: Int get() = 0

context(T) val <T> <!REDECLARATION!>differentlyNamedGenericsContextReceivers<!>: Int get() = 0
context(R) val <R> <!REDECLARATION!>differentlyNamedGenericsContextReceivers<!>: Int get() = 0

context(T) val <T> sameNamedGenericsDifferentBoundsContextReceivers: Int get() = 0
context(T) val <T : Any> sameNamedGenericsDifferentBoundsContextReceivers: String get() = ""

context(T) val <T> differentlyNamedGenericsDifferentBoundsContextReceivers: Int get() = 0
context(R) val <R : Any> differentlyNamedGenericsDifferentBoundsContextReceivers: String get() = ""

context(List<T>) val <T> sameNamedGenericsListDifferentBoundsContextReceivers: Int get() = 0
context(List<T>) val <T : Any> sameNamedGenericsListDifferentBoundsContextReceivers: String get() = ""

context(List<T>) val <T> differentlyNamedGenericsListDifferentBoundsContextReceivers: Int get() = 0
context(List<R>) val <R : Any> differentlyNamedGenericsListDifferentBoundsContextReceivers: String get() = ""

val Int.extensionVsContextReceiver: Int get() = 0
context(Int) val extensionVsContextReceiver: String get() = ""

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

    context(Int) val <!REDECLARATION!>simpleContextReceivers<!>: Int get() = 0
    context(Int) val <!REDECLARATION!>simpleContextReceivers<!>: Int get() = 0

    context(Int) val differentTypesContextReceivers: Int get() = 0
    context(String) val differentTypesContextReceivers: Int get() = 0

    context(T) val <T> <!REDECLARATION!>sameNamedGenericsContextReceivers<!>: Int get() = 0
    context(T) val <T> <!REDECLARATION!>sameNamedGenericsContextReceivers<!>: Int get() = 0

    context(T) val <T> <!REDECLARATION!>differentlyNamedGenericsContextReceivers<!>: Int get() = 0
    context(R) val <R> <!REDECLARATION!>differentlyNamedGenericsContextReceivers<!>: Int get() = 0

    context(T) val <T> sameNamedGenericsDifferentBoundsContextReceivers: Int get() = 0
    context(T) val <T : Any> sameNamedGenericsDifferentBoundsContextReceivers: String get() = ""

    context(T) val <T> differentlyNamedGenericsDifferentBoundsContextReceivers: Int get() = 0
    context(R) val <R : Any> differentlyNamedGenericsDifferentBoundsContextReceivers: String get() = ""

    context(List<T>) val <T> sameNamedGenericsListDifferentBoundsContextReceivers: Int get() = 0
    context(List<T>) val <T : Any> sameNamedGenericsListDifferentBoundsContextReceivers: String get() = ""

    context(List<T>) val <T> differentlyNamedGenericsListDifferentBoundsContextReceivers: Int get() = 0
    context(List<R>) val <R : Any> differentlyNamedGenericsListDifferentBoundsContextReceivers: String get() = ""

    val Int.extensionVsContextReceiver: Int get() = 0
    context(Int) val extensionVsContextReceiver: String get() = ""
}
