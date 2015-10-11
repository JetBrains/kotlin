// !DIAGNOSTICS: -UNUSED_PARAMETER

class CustomDelegate {
    fun get(thisRef: Any?, prop: PropertyMetadata): String = prop.name
    fun set(thisRef: Any?, prop: PropertyMetadata, value: String) {}
}

class OkDelegate {
    fun getValue(thisRef: Any?, prop: PropertyMetadata): String = prop.name
    fun setValue(thisRef: Any?, prop: PropertyMetadata, value: String) {}
}

class CustomDelegate2 {
    fun get(thisRef: Any?, prop: PropertyMetadata): String = prop.name
    fun set(thisRef: Any?, prop: PropertyMetadata, value: String) {}

    fun getValue(thisRef: Any?, prop: PropertyMetadata): Int = 5
    fun setValue(thisRef: Any?, prop: PropertyMetadata, value: Int) {}
}

class CustomDelegate3 {
    fun get(thisRef: Any?, prop: PropertyMetadata): String = prop.name
    fun set(thisRef: Any?, prop: PropertyMetadata, value: String) {}
}

fun OkDelegate.get(thisRef: Any?, prop: PropertyMetadata): Int = 4
fun OkDelegate.set(thisRef: Any?, prop: PropertyMetadata, value: Int) {}

fun CustomDelegate3.getValue(thisRef: Any?, prop: PropertyMetadata): Int = 4
fun CustomDelegate3.setValue(thisRef: Any?, prop: PropertyMetadata, value: Int) {}

class Example {

    var a by <!DELEGATE_RESOLVED_TO_DEPRECATED_CONVENTION, DELEGATE_RESOLVED_TO_DEPRECATED_CONVENTION!>CustomDelegate()<!>
    val aval by <!DELEGATE_RESOLVED_TO_DEPRECATED_CONVENTION!>CustomDelegate()<!>
    var b by OkDelegate()
    var c by CustomDelegate2()
    var d by CustomDelegate3()

    fun test() {
        requireString(a)
        requireString(aval)
        requireString(b)
        requireInt(c)
        requireInt(d)
    }

    fun requireString(s: String) {}
    fun requireInt(n: Int) {}

}