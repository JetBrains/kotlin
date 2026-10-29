// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class GenericDelegation<E>(val all: List<E>) : List<E> by all

@JvmInline
value class StringDelegation(val all: List<String>) : List<String> by all {
    override fun get(index: Int): String = all[index]
}

// LIGHT_ELEMENTS_NO_DECLARATION: GenericDelegation.class[add;add;addAll;addAll;clear;constructor-impl;contains-impl;containsAll-impl;equals-impl;equals-impl0;get-impl;getSize-impl;hashCode-impl;indexOf-impl;isEmpty-impl;iterator-impl;lastIndexOf-impl;listIterator-impl;listIterator-impl;remove;remove;removeAll;retainAll;set;subList-impl;toArray;toArray;toString-impl], StringDelegation.class[add;add;addAll;addAll;clear;constructor-impl;contains-impl;containsAll-impl;equals-impl;equals-impl0;get-impl;getSize-impl;hashCode-impl;indexOf-impl;isEmpty-impl;iterator-impl;lastIndexOf-impl;listIterator-impl;listIterator-impl;remove;remove;removeAll;retainAll;set;subList-impl;toArray;toArray;toString-impl]
