// ISSUE: KT-67221
// LANGUAGE: -ImprovedCapturedTypeApproximationInInference
// FILE: simple.kt
package simple

class Data<A>(val initial: A)

class Widget<B : Data<C>, C>(val data: B)

class WidgetWrapper<D : Data<E>, E>(val data: D)

fun foo(w: Widget<*, *>) {
    WidgetWrapper(data = w.data)
}


// FILE: deeperHierarchy.kt
// ISSUE: KT-64515
package deeperHierarchy

open class Data<A>(val initial: A)

class DataSub<A>(initial: A) : Data<A>(initial)

class Widget<B : DataSub<C>, C>(val data: B)

class WidgetWrapper<D : Data<E>, E>(val data: D)

fun foo(w: Widget<*, *>) {
    <!NEW_INFERENCE_ERROR!>WidgetWrapper(data = w.data)<!>
}