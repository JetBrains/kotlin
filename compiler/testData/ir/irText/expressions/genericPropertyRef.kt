class Value<T>(var value: T = null as T, var text: String? = null)

val <T> Value<T>.additionalText by DVal(Value<T>::text)

val <T> Value<T>.additionalValue by DVal(Value<T>::value)

class DVal(val kmember: Any) {
    operator fun getValue(t: Any?, p: Any) = 42
}

var recivier : Any? = "fail"
var value2 : Any? = "fail2"

var <T> T.bar : T
    get() = this
    set(value) { recivier = this; value2 = value}

val barRef = String?::bar
