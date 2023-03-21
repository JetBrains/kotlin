@OptIn(ExperimentalStdlibApi::class)
fun anyExample(@JsExternalArgument x : Any) = x

@OptIn(ExperimentalStdlibApi::class)
fun anyOrNullExample(@JsExternalArgument x : Any?) = x

@OptIn(ExperimentalStdlibApi::class)
fun <T> genericExample(@JsExternalArgument x : T) = x

@OptIn(ExperimentalStdlibApi::class)
fun <T> genericOrNullExample(@JsExternalArgument x : T?) = x

@OptIn(ExperimentalStdlibApi::class)
fun dynamicExample(@JsExternalArgument x : dynamic) = x

@OptIn(ExperimentalStdlibApi::class)
fun severalParams(@JsExternalArgument x : Any?, @JsExternalArgument y : Any?, @JsExternalArgument z : dynamic) = x ?: y ?: z

external interface ExternalInterface

fun boxExternalInterface(i: ExternalInterface) {
    anyExample(i)
    anyOrNullExample(i)
    genericExample(i)
    genericOrNullExample(i)
    dynamicExample(i)
}

fun boxExternalInterfaceOrNull(iOrNull: ExternalInterface?) {
    anyOrNullExample(iOrNull)
    genericExample(iOrNull)
    genericOrNullExample(iOrNull)
    dynamicExample(iOrNull)
}

external class ExternalClass

fun boxExternalClass(c: ExternalClass) {
    anyExample(c)
    anyOrNullExample(c)
    genericExample(c)
    genericOrNullExample(c)
    dynamicExample(c)
}

external object ExternalObject

fun boxExternalObject() {
    anyExample(ExternalObject)
    anyOrNullExample(ExternalObject)
    genericExample(ExternalObject)
    genericOrNullExample(ExternalObject)
    dynamicExample(ExternalObject)
}

interface Interface

fun boxInterface(i: Interface) {
    anyExample(i)
    anyOrNullExample(i)
    genericExample(i)
    genericOrNullExample(i)
    dynamicExample(i)
}

fun boxInterfaceOrNull(iOrNull: Interface?) {
    anyOrNullExample(iOrNull)
    genericExample(iOrNull)
    genericOrNullExample(iOrNull)
    dynamicExample(iOrNull)
}

class Class

fun boxInterface(c: Class) {
    anyExample(c)
    anyOrNullExample(c)
    genericExample(c)
    genericOrNullExample(c)
    dynamicExample(c)
}

object Object

fun boxObject() {
    anyExample(Object)
    anyOrNullExample(Object)
    genericExample(Object)
    genericOrNullExample(Object)
    dynamicExample(Object)
}

fun boxDynamic(d: dynamic) {
    anyExample(d)
    anyOrNullExample(d)
    genericExample(d)
    genericOrNullExample(d)
    dynamicExample(d)
}

fun boxPrimitiveTypes() {
    anyExample(1)
    anyExample(1.2)
    anyExample(true)
    anyExample("hello")
    anyExample({})
    anyExample({}())
    anyOrNullExample(null)
}

fun boxArgExpression(i: Interface, iOrNull: Interface?) {
    anyExample(iOrNull ?: i)
}

fun boxNamedArgExpression(i: Interface, iOrNull: Interface?, d: dynamic) {
    anyExample(x = iOrNull ?: i)

    severalParams(
        x = i,
        y = iOrNull ?: i,
        z = iOrNull ?: d
    )

    severalParams(
        z = iOrNull ?: d,
        x = i,
        y = iOrNull ?: i
    )

    severalParams(
        i,
        z = iOrNull ?: d,
        y = iOrNull ?: i
    )

    severalParams(
        x = i,
        y = ExternalObject,
        z = iOrNull ?: d
    )
}
