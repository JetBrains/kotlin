class Delegate {
    fun getValue(t: Any?, p: PropertyMetadata): Int = 1
}

class AImpl {
    val prop: Number by Delegate()
}

fun box(): String {
    return if(AImpl().prop == 1) "OK" else "fail"
}
