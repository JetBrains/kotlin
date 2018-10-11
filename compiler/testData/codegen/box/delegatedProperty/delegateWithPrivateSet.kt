// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// See KT-10107: 'Variable must be initialized' for delegate with private set

class My {
    var delegate: String by kotlin.properties.Delegates.notNull()
        private set

    init {
        delegate = "OK"
    }
}

fun box() = My().delegate
