// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
import kotlin.reflect.KProperty

class A

object Delegate {
    operator fun getValue(state: A, desc: KProperty<*>): Int  = 0
    operator fun setValue(state: A, desc: KProperty<*>, value: Int) {}
}

open class B {
    val A.foo: Int by Delegate
    var A.bar: Int by Delegate
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, objectDeclaration, operator,
propertyDeclaration, propertyDelegate, propertyWithExtensionReceiver, setter, starProjection */
