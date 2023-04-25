fun callRemovedOrNormalFunction(removed: Boolean): String = if (removed) removedFunction() else function()
fun callRemovedOrNormalFunctionOnObject(removed: Boolean): String = if (removed) A().removedFunction() else A().function()

fun readRemovedOrNormalProperty(removed: Boolean): String = if (removed) removedProperty else property
fun readRemovedOrNormalPropertyOnObject1(removed: Boolean): String = if (removed) A().removedProperty1 else A().property1
fun readRemovedOrNormalPropertyOnObject2(removed: Boolean): String = if (removed) A().removedProperty2 else A().property2

inline fun callInlinedRemovedFunction() = removedFunction()
inline fun readInlinedRemovedProperty() = removedProperty

class C2 : C() {
    override fun removedOpenFunction(): String = "O" // does not call super
    override val removedOpenProperty: String get() = "O" // does not call super
}

class I2 : I {
    override fun removedOpenFunction(): String = "K" // does not call super
    override val removedOpenProperty: String get() = "K" // does not call super
}
