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
                or(<!DEBUG_INFO_SMARTCAST!>currentValue<!>, <!DEBUG_INFO_SMARTCAST!>appendedValue<!>)
            }
            map[field] = <!DEBUG_INFO_SMARTCAST!>currentValue<!>
        }
    }

    fun or(left: T, right: T): T = left
}
