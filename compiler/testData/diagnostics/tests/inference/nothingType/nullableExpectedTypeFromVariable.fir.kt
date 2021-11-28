// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +NewInference
// WITH_STDLIB

class Foo<T> {
    private fun append(map: MutableMap<String, T>, field: String, appendedValue: T?) {
        if (appendedValue != null) {
            var currentValue: T? = map[field]
            currentValue = if (currentValue == null) {
                appendedValue
            } else {
                or(currentValue, appendedValue)
            }
            map[field] = currentValue
        }
    }

    fun or(left: T, right: T): T = left
}
