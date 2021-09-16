// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

@file:Suppress("RESERVED_VAR_PROPERTY_OF_VALUE_CLASS")

open class EntityFactory<E>(val size: Int, val factory: (Int) -> E)

class EntityContext {
    var d = DoubleArray(16)
        private set
    var size: Int = 0
        private set
    fun <E> create(entity: EntityFactory<E>): E {
        val i = size
        size += entity.size
        if (size > d.size) d = d.copyOf(maxOf(2 * d.size, size))
        return entity.factory(i)
    }
}

@JvmInline value class EDouble(private val i: Int) {
    companion object Factory : EntityFactory<EDouble>(1, ::EDouble)

    context(EntityContext)
    var value: Double
        get() = d[i]
        set(value) { d[i] = value }
}

@JvmInline value class EVec3(private val i: Int) {
    companion object Factory : EntityFactory<EVec3>(3, ::EVec3)

    context(EntityContext)
    var x: Double
        get() = d[i]
        set(value) { d[i] = value }

    context(EntityContext)
    var y: Double
        get() = d[i + 1]
        set(value) { d[i + 1] = value }

    context(EntityContext)
    var z: Double
        get() = d[i + 2]
        set(value) { d[i + 2] = value }
}

context(EntityContext)
fun EVec3.str(): String =
    "[$x, $y, $z]"

context(EntityContext)
operator fun EVec3.plusAssign(v: EVec3) {
    x += v.x
    y += v.y
    z += v.z
}

fun box(): String = with(EntityContext()) {
    val v0 = create(EVec3)
    v0.x = 1.0
    v0.y = 2.0
    v0.z = 3.0
    val v1 = create(EVec3)
    v1.x = 2.0
    v1.y = 0.0
    v1.z = 4.0
    v1 += v0
    if (v1.x == 3.0 && v1.y == 2.0 && v1.z == 7.0) "OK" else "fail"
}