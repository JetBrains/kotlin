// STDLIB_JDK8
// FULL_JDK
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED
package test

@JvmInline
value class Id(val id: Long)

class Delegating(private val delegate: MutableCollection<Id>) : MutableCollection<Id> by delegate

class DelegatingWithOverrides(private val delegate: MutableCollection<Id>) : MutableCollection<Id> by delegate {
    override fun remove(element: Id): Boolean = delegate.remove(element)

    override fun contains(element: Id): Boolean = delegate.contains(element)
}

// LIGHT_ELEMENTS_NO_DECLARATION: Delegating.class[add-kRzuEh0;contains-kRzuEh0;remove-kRzuEh0;size;toArray;toArray], DelegatingWithOverrides.class[add-kRzuEh0;contains-kRzuEh0;remove-kRzuEh0;size;toArray;toArray], Id.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
