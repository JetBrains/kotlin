class Delegate {
    var count = 0
    fun getValue(t: Any?, p: PropertyMetadata) {}
    fun propertyDelegated(vararg p: PropertyMetadata) { count++ }
}

val delegate = Delegate()

val prop1 by delegate
val prop2 by delegate

fun box(): String {
    return if(delegate.count == 2) "OK" else "fail"
}
