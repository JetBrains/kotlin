// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE

import kotlin.reflect.KProperty

object Foo
object Bar
object Baz

operator fun Foo.provideDelegate(receiver: Any?, property: KProperty<*>) = this
operator fun Bar.provideDelegate(receiver: Any?, property: KProperty<*>) = this

operator fun Foo.getValue(nothing: Any?, property: KProperty<*>): Any = TODO()
operator fun Bar.getValue(nothing: Any?, property: KProperty<*>): Any = TODO()
operator fun Baz.getValue(nothing: Any?, property: KProperty<*>): Any = TODO()

fun test() {
    val bar by Baz
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, localProperty, nullableType, objectDeclaration,
operator, propertyDeclaration, propertyDelegate, starProjection, thisExpression */
