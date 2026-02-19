import kotlin.reflect.KMutableProperty

class SimpleVarClass(var constructorVariable: Boolean) {
    var memberVariable: Boolean = constructorVariable
}

fun moreFun(a: KMutableProperty<Boolean>) = a

fun testMutableProp() {
    moreFun(SimpleVarClass::constructorVariable)
    moreFun(SimpleVarClass::memberVariable)
}
