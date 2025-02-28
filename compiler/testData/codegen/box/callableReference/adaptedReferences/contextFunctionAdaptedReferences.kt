// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
class A

fun funWithContextAndValueType(x: context(A) (Int) -> Unit) {}
fun funWithContextAndExtensionType(x: context(A) Int.() -> Unit) {}
fun funWithContextsType(x: context(A, Int) () -> Unit) {}

fun valueParamFun(a: A, i: Int) { }
fun A.extensionFun(i: Int) { }

inline fun inlineValueParamFun(a: A, i: Int) { }
inline fun A.inlineExtensionFun(i: Int) { }

inline fun <reified T> inlineReifiedValueParamFun(a: T, i: Int) { }
inline fun <reified T> T.inlineReifiedExtensionFun(i: Int) { }

fun box(): String {
    funWithContextAndValueType(::valueParamFun)
    funWithContextAndValueType(A::extensionFun)
    funWithContextAndValueType(::inlineValueParamFun)
    funWithContextAndValueType(A::inlineExtensionFun)
    funWithContextAndValueType(::inlineReifiedValueParamFun)
    funWithContextAndValueType(A::inlineReifiedExtensionFun)

    funWithContextAndExtensionType(::valueParamFun)
    funWithContextAndExtensionType(A::extensionFun)
    funWithContextAndExtensionType(::inlineValueParamFun)
    funWithContextAndExtensionType(A::inlineExtensionFun)
    funWithContextAndExtensionType(::inlineReifiedValueParamFun)
    funWithContextAndExtensionType(A::inlineReifiedExtensionFun)

    funWithContextsType(::valueParamFun)
    funWithContextsType(A::extensionFun)
    funWithContextsType(::inlineValueParamFun)
    funWithContextsType(A::inlineExtensionFun)
    funWithContextsType(::inlineReifiedValueParamFun)
    funWithContextsType(A::inlineReifiedExtensionFun)
    return "OK"
}