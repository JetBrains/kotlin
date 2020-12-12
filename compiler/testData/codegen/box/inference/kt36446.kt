// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
// !LANGUAGE: +NewInference
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// WITH_REFLECT

import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KProperty

fun box(): String {
    definition<Transaction> {
        conversion<Unit> {
            val offset by argument<Int> {
                // UnsupportedOperationException: no descriptor for type constructor of IntegerLiteralType[Int,Long,Byte,Short]
                defaultInt(0)
            }
        }
    }
    return "OK"
}

interface Transaction
fun <Transaction> definition(configure: DefinitionBuilder<Transaction>.() -> Unit): Unit {}
class ArgumentBuilder<Value> {
    fun defaultInt(default: Int): Unit {}
}
class ConversionBuilder<Value> {
    fun <ArgumentValue> argument(
        configure: ArgumentBuilder<ArgumentValue>.() -> Unit
    ): ArgumentDefinition<ArgumentValue> = null as ArgumentDefinition<ArgumentValue>
}
class DefinitionBuilder<Transaction> {
    @OptIn(ExperimentalTypeInference::class)
    fun <Value> conversion(@BuilderInference configure: ConversionBuilder<Value>.() -> Unit): Unit {}
}
interface ArgumentDefinition<Value> {
    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ArgumentReference<Value>
}
interface ArgumentReference<out Value> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Value
}
