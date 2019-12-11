// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.*

interface Parent
class Child : Parent

class ChildHolder(var child: Child)

interface Inv<T>

class Form {
    fun <F> get0(field: KMutableProperty<F>): Inv<F> = TODO()
    fun <F> get1(field: KProperty<F>): Inv<F> = TODO()
    fun <F> get2(field: KCallable<F>): Inv<F> = TODO()
    fun <F> get3(field: KMutableProperty1<*, F>): Inv<F> = TODO()
}

fun <T : Parent> radio(field: Inv<T>) {}

fun test(form: Form) {
    radio(form.get0(ChildHolder::child))
    radio(form.get1(ChildHolder::child))
    radio(form.get2(ChildHolder::child))
    radio(form.get3(ChildHolder::child))
}