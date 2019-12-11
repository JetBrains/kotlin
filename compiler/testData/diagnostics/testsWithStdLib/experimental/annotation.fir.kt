// !USE_EXPERIMENTAL: kotlin.Experimental
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING)
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS,
        AnnotationTarget.VALUE_PARAMETER)
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
@EAnno fun function() {}

@UseExperimental(ExperimentalAPI::class)
fun parameter(@EAnno p: String) {}

@UseExperimental(ExperimentalAPI::class)
fun parameterType(p: @EAnno String) {}

@UseExperimental(ExperimentalAPI::class)
fun returnType(): @EAnno Unit {}

@UseExperimental(ExperimentalAPI::class)
@EAnno val property = ""

@UseExperimental(ExperimentalAPI::class)
@EAnno typealias Typealias = Unit

@UseExperimental(ExperimentalAPI::class)
@EAnno class Klass

@UseExperimental(ExperimentalAPI::class)
annotation class AnnotationArgument(val p: EAnno)

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

@EAnno fun function() {}

fun parameter(@EAnno p: String) {}

fun parameterType(p: @EAnno String) {}

fun returnType(): @EAnno Unit {}

@EAnno val property = ""

@EAnno typealias Typealias = Unit

@EAnno class Klass

annotation class AnnotationArgument(val p: EAnno)

fun insideBody() {
    @EAnno fun local() {}
}

fun inDefaultArgument(f: () -> Unit = @EAnno fun() {}) {}

val inProperty = @EAnno fun() {}

val inPropertyAccessor: () -> Unit
    get() = @EAnno fun() {}
