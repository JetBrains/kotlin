// !LANGUAGE: +ContextReceivers
// IGNORE_BACKEND_K1: JS, NATIVE
// SKIP_NODE_JS

open class A {
    fun instance() = true
    val instanceProp = true

    companion object : A() {
        fun companion() = true
        val companionProp = true
    }
}

fun A.ext() = true
val A.extProp get() = true

fun A.Companion.companionExt() = true
val A.Companion.companionExtProp get() = true

inline fun call(f: () -> Boolean) = f()
inline fun callExtension(f: A.() -> Boolean, receiver: A) = receiver.f()
inline fun callParameter(f: (A) -> Boolean, parameter: A) = f(parameter)
inline fun callContext(f: context(A) () -> Boolean, receiver: A) = f(receiver)

fun box(): String {
    if (!call(A::instance)) return "Fail bound function 1"
    if (!call(A.Companion::instance)) return "Fail bound function 2"
    if (!call(A::companion)) return "Fail bound function 3"
    if (!call(A.Companion::companion)) return "Fail bound function 4"
    if (!call(A::ext)) return "Fail bound function 5"
    if (!call(A.Companion::ext)) return "Fail bound function 6"
    if (!call(A::companionExt)) return "Fail bound function 7"
    if (!call(A.Companion::companionExt)) return "Fail bound function 8"

    if (!call(A::instanceProp)) return "Fail bound prop 1"
    if (!call(A.Companion::instanceProp)) return "Fail bound prop 2"
    if (!call(A::companionProp)) return "Fail bound prop 3"
    if (!call(A.Companion::companionProp)) return "Fail bound prop 4"
    if (!call(A::extProp)) return "Fail bound prop 5"
    if (!call(A.Companion::extProp)) return "Fail bound prop 6"
    if (!call(A::companionExtProp)) return "Fail bound prop 7"
    if (!call(A.Companion::companionExtProp)) return "Fail bound prop 8"

    if (!callParameter(A::instance, A.Companion)) return "Fail unbound 1"
    if (!callParameter(A::ext, A.Companion)) return "Fail unbound 2"
    if (!callParameter(A::instanceProp, A.Companion)) return "Fail unbound 3"
    if (!callParameter(A::extProp, A.Companion)) return "Fail unbound 4"
    if (!callExtension(A::instance, A.Companion)) return "Fail unbound 5"
    if (!callExtension(A::ext, A.Companion)) return "Fail unbound 6"
    if (!callExtension(A::instanceProp, A.Companion)) return "Fail unbound 7"
    if (!callExtension(A::extProp, A.Companion)) return "Fail unbound 8"
    if (!callContext(A::instance, A.Companion)) return "Fail unbound 5"
    if (!callContext(A::ext, A.Companion)) return "Fail unbound 6"
    if (!callContext(A::instanceProp, A.Companion)) return "Fail unbound 7"
    if (!callContext(A::extProp, A.Companion)) return "Fail unbound 8"

    with (A) {
        if (!call(::instance)) return "Fail implicit bound function 1"
        if (!call(::companion)) return "Fail implicit bound function 2"
        if (!call(::ext)) return "Fail implicit bound function 3"
        if (!call(::companionExt)) return "Fail implicit bound function 4"
        if (!call(::instanceProp)) return "Fail imlicit bound prop 5"
        if (!call(::companionProp)) return "Fail imlicit bound prop 6"
        if (!call(::extProp)) return "Fail imlicit bound prop 7"
        if (!call(::companionExtProp)) return "Fail imlicit bound prop 8"
    }

    val instance = A::instance
    val ext = A::ext
    val instanceProp = A::instanceProp
    val extProp = A::extProp

    if (!callParameter(instance, A.Companion)) return "Fail unbound variable 1"
    if (!callParameter(ext, A.Companion)) return "Fail unbound variable 2"
    if (!callParameter(instanceProp, A.Companion)) return "Fail unbound variable 3"
    if (!callParameter(extProp, A.Companion)) return "Fail unbound variable 4"

    return "OK"
}
