// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class ApiMarker

@SubclassOptInRequired(ApiMarker::class)
open class OpenKlass

@SubclassOptInRequired(ApiMarker::class)
abstract class AbstractKlass

@SubclassOptInRequired(ApiMarker::class)
interface Interface

class OuterKlass {

    @SubclassOptInRequired(ApiMarker::class)
    open class NestedOpenKlass

    @SubclassOptInRequired(ApiMarker::class)
    abstract class NestedAbstractKlass

    @SubclassOptInRequired(ApiMarker::class)
    interface NestedInterface

    @SubclassOptInRequired(ApiMarker::class)
    open inner class InnerOpenKlass

    @SubclassOptInRequired(ApiMarker::class)
    abstract inner class InnerAbstractKlass

}
