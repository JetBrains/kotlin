// FIR_IDENTICAL

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
    anyExample(<!JS_EXTERNAL_ARGUMENT!>i<!>)
    anyOrNullExample(<!JS_EXTERNAL_ARGUMENT!>i<!>)
    genericExample(<!JS_EXTERNAL_ARGUMENT!>i<!>)
    genericOrNullExample(<!JS_EXTERNAL_ARGUMENT!>i<!>)
    dynamicExample(<!JS_EXTERNAL_ARGUMENT!>i<!>)
}

fun boxInterfaceOrNull(iOrNull: Interface?) {
    anyOrNullExample(<!JS_EXTERNAL_ARGUMENT!>iOrNull<!>)
    genericExample(<!JS_EXTERNAL_ARGUMENT!>iOrNull<!>)
    genericOrNullExample(<!JS_EXTERNAL_ARGUMENT!>iOrNull<!>)
    dynamicExample(<!JS_EXTERNAL_ARGUMENT!>iOrNull<!>)
}

class Class

fun boxInterface(c: Class) {
    anyExample(<!JS_EXTERNAL_ARGUMENT!>c<!>)
    anyOrNullExample(<!JS_EXTERNAL_ARGUMENT!>c<!>)
    genericExample(<!JS_EXTERNAL_ARGUMENT!>c<!>)
    genericOrNullExample(<!JS_EXTERNAL_ARGUMENT!>c<!>)
    dynamicExample(<!JS_EXTERNAL_ARGUMENT!>c<!>)
}

object Object

fun boxObject() {
    anyExample(<!JS_EXTERNAL_ARGUMENT!>Object<!>)
    anyOrNullExample(<!JS_EXTERNAL_ARGUMENT!>Object<!>)
    genericExample(<!JS_EXTERNAL_ARGUMENT!>Object<!>)
    genericOrNullExample(<!JS_EXTERNAL_ARGUMENT!>Object<!>)
    dynamicExample(<!JS_EXTERNAL_ARGUMENT!>Object<!>)
}

fun boxDynamic(d: dynamic) {
    anyExample(<!JS_EXTERNAL_ARGUMENT!>d<!>)
    anyOrNullExample(<!JS_EXTERNAL_ARGUMENT!>d<!>)
    genericExample(<!JS_EXTERNAL_ARGUMENT!>d<!>)
    genericOrNullExample(<!JS_EXTERNAL_ARGUMENT!>d<!>)
    dynamicExample(<!JS_EXTERNAL_ARGUMENT!>d<!>)
}

fun boxPrimitiveTypes() {
    anyExample(<!JS_EXTERNAL_ARGUMENT!>1<!>)
    anyExample(<!JS_EXTERNAL_ARGUMENT!>1.2<!>)
    anyExample(<!JS_EXTERNAL_ARGUMENT!>true<!>)
    anyExample(<!JS_EXTERNAL_ARGUMENT!>"hello"<!>)
    anyExample(<!JS_EXTERNAL_ARGUMENT!>{}<!>)
    anyExample(<!JS_EXTERNAL_ARGUMENT!>{}()<!>)
    anyOrNullExample(<!JS_EXTERNAL_ARGUMENT!>null<!>)
}

fun boxArgExpression(i: Interface, iOrNull: Interface?) {
    anyExample(<!JS_EXTERNAL_ARGUMENT!>iOrNull ?: i<!>)
}

fun boxNamedArgExpression(i: Interface, iOrNull: Interface?, d: dynamic) {
    anyExample(x = <!JS_EXTERNAL_ARGUMENT!>iOrNull ?: i<!>)

    severalParams(
        x = <!JS_EXTERNAL_ARGUMENT!>i<!>,
        y = <!JS_EXTERNAL_ARGUMENT!>iOrNull ?: i<!>,
        z = <!JS_EXTERNAL_ARGUMENT!>iOrNull ?: d<!>
    )

    severalParams(
        z = <!JS_EXTERNAL_ARGUMENT!>iOrNull ?: d<!>,
        x = <!JS_EXTERNAL_ARGUMENT!>i<!>,
        y = <!JS_EXTERNAL_ARGUMENT!>iOrNull ?: i<!>
    )

    severalParams(
        <!JS_EXTERNAL_ARGUMENT!>i<!>,
        z = <!JS_EXTERNAL_ARGUMENT!>iOrNull ?: d<!>,
        y = <!JS_EXTERNAL_ARGUMENT!>iOrNull ?: i<!>
    )

    severalParams(
        x = <!JS_EXTERNAL_ARGUMENT!>i<!>,
        y = ExternalObject,
        z = <!JS_EXTERNAL_ARGUMENT!>iOrNull ?: d<!>
    )
}
