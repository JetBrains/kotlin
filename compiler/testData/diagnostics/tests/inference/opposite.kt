package a

trait Persistent
trait PersistentFactory<T>

class Relation<Source: Persistent, Target: Persistent>(
        val sources: PersistentFactory<Source>,
        val targets: PersistentFactory<Target>
) {
    fun opposite() = Relation(targets, sources)
}
