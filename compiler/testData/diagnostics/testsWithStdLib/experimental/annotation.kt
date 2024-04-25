// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: api.kt

package api

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS,
        AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalAPI

@ExperimentalAPI
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS,
        AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
annotation class EAnno

// FILE: usage-propagate.kt

package usage1

import api.*

@ExperimentalAPI
@EAnno fun function() {}

@ExperimentalAPI
fun parameter(@EAnno p: String) {}

@ExperimentalAPI
fun parameterType(p: <!WRONG_ANNOTATION_TARGET!>@EAnno<!> String) {}

@ExperimentalAPI
fun returnType(): <!WRONG_ANNOTATION_TARGET!>@EAnno<!> Unit {}

@ExperimentalAPI
@EAnno val property = ""

@ExperimentalAPI
@EAnno typealias Typealias = Unit

@ExperimentalAPI
@EAnno class Klass

@ExperimentalAPI
annotation class AnnotationArgument(val p: EAnno)

@ExperimentalAPI
fun insideBody() {
    @EAnno fun local() {}
}

@ExperimentalAPI
fun inDefaultArgument(f: () -> Unit = @EAnno fun() {}) {}

@ExperimentalAPI
val inProperty = @EAnno fun() {}

@ExperimentalAPI
val inPropertyAccessor: () -> Unit
    get() = @EAnno fun() {}

// FILE: usage-use.kt

package usage2

import api.*

@OptIn(ExperimentalAPI::class)
@EAnno fun function() {}

@OptIn(ExperimentalAPI::class)
fun parameter(@EAnno p: String) {}

@OptIn(ExperimentalAPI::class)
fun parameterType(p: <!WRONG_ANNOTATION_TARGET!>@EAnno<!> String) {}

@OptIn(ExperimentalAPI::class)
fun returnType(): <!WRONG_ANNOTATION_TARGET!>@EAnno<!> Unit {}

@OptIn(ExperimentalAPI::class)
@EAnno val property = ""

@OptIn(ExperimentalAPI::class)
@EAnno typealias Typealias = Unit

@OptIn(ExperimentalAPI::class)
@EAnno class Klass

@OptIn(ExperimentalAPI::class)
annotation class AnnotationArgument(val p: EAnno)

fun insideBody() {
    @OptIn(ExperimentalAPI::class) @EAnno fun local() {}
}

fun inDefaultArgument(@OptIn(ExperimentalAPI::class) f: () -> Unit = @EAnno fun() {}) {}

@OptIn(ExperimentalAPI::class)
val inProperty = @EAnno fun() {}

val inPropertyAccessor: () -> Unit
    @OptIn(ExperimentalAPI::class)
    get() = @EAnno fun() {}

// FILE: usage-none.kt

package usage3

import api.*

@<!OPT_IN_USAGE!>EAnno<!> fun function() {}

fun parameter(@<!OPT_IN_USAGE!>EAnno<!> p: String) {}

fun parameterType(p: <!WRONG_ANNOTATION_TARGET!>@<!OPT_IN_USAGE!>EAnno<!><!> String) {}

fun returnType(): <!WRONG_ANNOTATION_TARGET!>@<!OPT_IN_USAGE!>EAnno<!><!> Unit {}

@<!OPT_IN_USAGE!>EAnno<!> val property = ""

@<!OPT_IN_USAGE!>EAnno<!> typealias Typealias = Unit

@<!OPT_IN_USAGE!>EAnno<!> class Klass

annotation class AnnotationArgument(val p: <!OPT_IN_USAGE!>EAnno<!>)

fun insideBody() {
    @<!OPT_IN_USAGE!>EAnno<!> fun local() {}
}

fun inDefaultArgument(f: () -> Unit = @<!OPT_IN_USAGE!>EAnno<!> fun() {}) {}

val inProperty = @<!OPT_IN_USAGE!>EAnno<!> fun() {}

val inPropertyAccessor: () -> Unit
    get() = @<!OPT_IN_USAGE!>EAnno<!> fun() {}
