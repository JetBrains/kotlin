// FIR_IDENTICAL
// ISSUE: KT-67221

class Data<A>(val initial: A)

class Widget<B : Data<C>, C>(val data: B)

class WidgetWrapper<D : Data<E>, E>(val data: D)

fun foo(w: Widget<*, *>) {
    WidgetWrapper(data = w.data)
}
