// FIR_IDENTICAL
// !OPT_IN: kotlin.js.ExperimentalJsExport

@JsExport
interface GenericInterface1<T> {
    var x: String
}

@JsExport
interface GenericInterface2<T> {
    var x: T
}

@JsExport
interface Interface1 {
    var x: String
}

@JsExport
interface Interface2 {
    var x: String
}

@JsExport
abstract class AbstractClass : Interface1 {
    override var x = "AC"
}

@JsExport
open class OpenClass1 : AbstractClass(), Interface2

@JsExport
open class OpenClass2 : AbstractClass(), GenericInterface1<Int>

@JsExport
open class OpenClass3 : AbstractClass(), GenericInterface2<String>

@JsExport
open class OpenClass4 : AbstractClass(), Interface1, Interface2, GenericInterface1<Int>, GenericInterface2<String>
