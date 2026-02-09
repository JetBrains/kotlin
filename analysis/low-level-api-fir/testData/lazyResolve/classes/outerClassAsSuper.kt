abstract class ConstantValue<out T>(open val value: T)

class KClassValue(value: Value) : ConstantValue<KClassValue.Value>(value) {
    class Value {
        class Norm<caret>alClass : Value()
    }
}
