// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE_FEATURE_TOGGLED: AllowAnnotationsOnArgumentsOfAnnotations

import kotlin.reflect.KClass

@Repeatable
annotation class SimpleAnno(val kClass: KClass<*>)
@Repeatable
annotation class VarargAnno(vararg val kClass: KClass<*>)
@Repeatable
annotation class ArrayAnno(val kClass: Array<KClass<*>>)

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class Marker

@Marker
class Marked

@SimpleAnno(<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@OptIn(Marker::class)<!> Marked::class)
@SimpleAnno((@OptIn(Marker::class) Marked)::class)
@VarargAnno(@OptIn(Marker::class) Marked::class, String::class, @OptIn(Marker::class) Marked::class)
@ArrayAnno(arrayOf(String::class, *<!ANNOTATION_ON_ANNOTATION_ARGUMENT!>@OptIn(Marker::class)<!>[String::class, Marked::class]))
fun test() = Unit

@VarargAnno(@OptIn(Marker::class) String::class, <!OPT_IN_USAGE!>Marked<!>::class)
fun missed() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, classReference, collectionLiteral, functionDeclaration,
outProjection, primaryConstructor, propertyDeclaration, starProjection, vararg */
