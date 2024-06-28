// WITH_STDLIB

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

inline fun <reified Self : DatabaseEntity, reified Target : DatabaseEntity> Self.parent(
    property: KProperty1<Target, MutableCollection<Self>>): Delegate<Self, Target?> = TODO()

class GitLabBuildProcessor: DatabaseEntity {
    var processor by parent(GitLabChangesProcessor::buildProcessors)
}

interface DatabaseEntity: Entity
interface Entity
interface ResourceFactory<T, R>
interface ValueFilter<K>

interface Delegate<R : Entity, T> : ReadWriteProperty<R, T>, ValueFilter<R> {
    infix fun name(desc: KProperty<*>): String
infix fun by(name: String): Delegate<R, T>
infix fun resource(factory: ResourceFactory<R, T>): Delegate<R, T>
infix fun filter(filter: (R, Any?) -> Boolean): Delegate<R, T>
}

class GitLabChangesProcessor: DatabaseEntity {
    var buildProcessors by child_many(
        GitLabBuildProcessor::class.java,
        GitLabBuildProcessor::<!INAPPLICABLE_CANDIDATE!>processor<!>
    )
}

fun <Self : DatabaseEntity, Target : DatabaseEntity> Self.child_many(
    clazz: Class<Target>, property: KProperty1<Target, Self?>, name: String = property.name
): Delegate<Self, MutableCollection<Target>> = TODO() // Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly
