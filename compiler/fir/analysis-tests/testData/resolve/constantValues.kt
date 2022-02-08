abstract class ConstantValue<out T>(open val value: T)

data class ClassLiteralValue(val classId: ClassId, val arrayNestedness: Int)

class ClassId
class KotlinType

class KClassValue(value: Value) : ConstantValue<KClassValue.Value>(value) {
    sealed class Value {
        data class NormalClass(val value: ClassLiteralValue) : Value() {
            <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val classId: ClassId<!>
            <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val arrayDimensions: Int<!>
        }

        data class LocalClass(val type: KotlinType) : Value()
    }

    fun getArgumentType(): KotlinType {
        when (value) {
            is Value.LocalClass -> return value.type
            is Value.NormalClass -> {
                val (classId, arrayDimensions) = value.value
            }
        }
    <!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
}
