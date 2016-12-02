// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun createDelegate(x: Any?, p: KProperty<*>) {}

operator fun Any.createDelegate(x: Any?, p: KProperty<*>) {}

operator fun Any.createDelegate(x: Any?, p: Any) {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Any.createDelegate(x: Any?, p: Int) {}

class Host1 {
    operator fun createDelegate(x: Any?, p: KProperty<*>) {}
}

class Host2 {
    operator fun Any.createDelegate(x: Any?, p: KProperty<*>) {}
}

class Host3 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun createDelegate(x: Any?, p: KProperty<*>, foo: Int) {}
}

class Host4 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun createDelegate(x: Any?, p: KProperty<*>, foo: Int = 0) {}
}

class Host5 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun createDelegate(x: Any?, p: KProperty<*>, vararg foo: Int) {}
}

