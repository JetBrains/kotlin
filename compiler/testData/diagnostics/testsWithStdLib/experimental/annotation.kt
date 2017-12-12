// !API_VERSION: 1.3
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.COMPILATION])
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS,
        AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
annotation class ExperimentalAPI

@ExperimentalAPI
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS,
        AnnotationTarget.VALUE_PARAMETER)
annotation class EAnno

// FILE: usage-propagate.kt

package usage1

import api.*

@ExperimentalAPI
@EAnno fun function() {}

@ExperimentalAPI
fun parameter(@EAnno p: String) {}

@ExperimentalAPI
fun parameterType(p: @EAnno String) {}

@ExperimentalAPI
fun returnType(): @EAnno Unit {}

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

@UseExperimental(ExperimentalAPI::class)
@<!EXPERIMENTAL_API_USAGE!>EAnno<!> fun function() {}

@UseExperimental(ExperimentalAPI::class)
fun parameter(@<!EXPERIMENTAL_API_USAGE!>EAnno<!> p: String) {}

@UseExperimental(ExperimentalAPI::class)
fun parameterType(p: @<!EXPERIMENTAL_API_USAGE!>EAnno<!> String) {}

@UseExperimental(ExperimentalAPI::class)
fun returnType(): @<!EXPERIMENTAL_API_USAGE!>EAnno<!> Unit {}

@UseExperimental(ExperimentalAPI::class)
@<!EXPERIMENTAL_API_USAGE!>EAnno<!> val property = ""

<!WRONG_ANNOTATION_TARGET!>@UseExperimental(ExperimentalAPI::class)<!>
@<!EXPERIMENTAL_API_USAGE!>EAnno<!> typealias Typealias = Unit

@UseExperimental(ExperimentalAPI::class)
@<!EXPERIMENTAL_API_USAGE!>EAnno<!> class Klass

@UseExperimental(ExperimentalAPI::class)
annotation class AnnotationArgument(val p: <!EXPERIMENTAL_API_USAGE!>EAnno<!>)

fun insideBody() {
    @UseExperimental(ExperimentalAPI::class) @EAnno fun local() {}
}

fun inDefaultArgument(@UseExperimental(ExperimentalAPI::class) f: () -> Unit = @EAnno fun() {}) {}

@UseExperimental(ExperimentalAPI::class)
val inProperty = @EAnno fun() {}

val inPropertyAccessor: () -> Unit
    @UseExperimental(ExperimentalAPI::class)
    get() = @EAnno fun() {}

// FILE: usage-none.kt

package usage3

import api.*

@<!EXPERIMENTAL_API_USAGE!>EAnno<!> fun function() {}

fun parameter(@<!EXPERIMENTAL_API_USAGE!>EAnno<!> p: String) {}

fun parameterType(p: @<!EXPERIMENTAL_API_USAGE!>EAnno<!> String) {}

fun returnType(): @<!EXPERIMENTAL_API_USAGE!>EAnno<!> Unit {}

@<!EXPERIMENTAL_API_USAGE!>EAnno<!> val property = ""

@<!EXPERIMENTAL_API_USAGE!>EAnno<!> typealias Typealias = Unit

@<!EXPERIMENTAL_API_USAGE!>EAnno<!> class Klass

annotation class AnnotationArgument(val p: <!EXPERIMENTAL_API_USAGE!>EAnno<!>)

fun insideBody() {
    @<!EXPERIMENTAL_API_USAGE!>EAnno<!> fun local() {}
}

fun inDefaultArgument(f: () -> Unit = @<!EXPERIMENTAL_API_USAGE!>EAnno<!> fun() {}) {}

val inProperty = @<!EXPERIMENTAL_API_USAGE!>EAnno<!> fun() {}

val inPropertyAccessor: () -> Unit
    get() = @<!EXPERIMENTAL_API_USAGE!>EAnno<!> fun() {}
