// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-50776
interface Entities<Target> : MutableCollection<Target>, Sequence<Target>

abstract class StringEntities : Entities<String> {
    fun foo() {
        forEach {
            println(it)
        }
    }
}