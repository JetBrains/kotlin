// IGNORE_NATIVE: compatibilityTestMode=OldArtifactNewCompiler
// ISSUE: KT-74999

interface Traversable {
    fun foo(): String = "fail"
}
interface Entity<S : Entity<S>> : Traversable {
    fun <T : S> isEqualTo(): T
}

interface Path : Traversable

fun <K> select(x: K, y: K): K = x

fun foo(path: Path, e: Entity<*>): String {
    return select(e.isEqualTo(), path).foo()
}

class EntityImpl : Entity<EntityImpl>, Path {
    override fun <T : EntityImpl> isEqualTo(): T = this as T
    override fun foo(): String = "OK"
}

fun box(): String {
    return foo(object : Path {}, EntityImpl())
}
