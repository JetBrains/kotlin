// WITH_STDLIB
// ISSUE: KT-57288
import kotlin.properties.ReadWriteProperty

interface Delegate<R, T> : ReadWriteProperty<R, T> {
    infix fun resource(factory: Factory<R, T>): Delegate<R, T>
}

interface Factory<Source, Target>

interface Some

fun <Self : Some, Target : Some> Self.delegateOf(clazz: Class<Target>): Delegate<Self, Target?> = null!!

abstract class SomeImpl<R : Some> : Some {
    abstract val type: Class<R>

    var bundle by delegateOf(type) resource getFactory()

    fun <M : SomeImpl<T>, T : Some> getFactory(): Factory<M, T?> = null!!
}
