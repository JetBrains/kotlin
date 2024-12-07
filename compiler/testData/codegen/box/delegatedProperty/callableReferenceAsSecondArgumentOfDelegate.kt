// WITH_STDLIB
// ISSUE: KT-65165

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private class TransformedConfigPropertyString(
    defaultValue: String,
    private val transform: (String) -> Regex
) : ReadOnlyProperty<SampleClass, Regex> {
    override fun getValue(thisRef: SampleClass, property: KProperty<*>): Regex {
        return transform("string")
    }
}

class SampleClass {
    val ignoreStringsRegex: Regex by TransformedConfigPropertyString("$^", String::toRegex)
}

fun box(): String {
    SampleClass().ignoreStringsRegex
    return "OK"
}