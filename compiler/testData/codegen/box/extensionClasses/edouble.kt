// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

class EntityContext {
    var d = DoubleArray(16)
}

class EDouble(val i: Int) {
    context(EntityContext)
    var value:   Double
        get() = d[i]
        set(value) { d[i] = value }
}

fun box(): String {
    val entityContext = EntityContext()
    with(entityContext) {
        val eDouble = EDouble(0)
        eDouble.value = .2
        return if (eDouble.value == .2) "OK" else "fail"
    }
}