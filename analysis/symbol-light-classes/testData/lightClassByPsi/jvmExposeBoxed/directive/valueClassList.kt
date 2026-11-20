// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class MyInlineClass(val all: List<String>) : List<String> {
    override val size: Int
        get() = all.size

    override fun isEmpty(): Boolean = all.isEmpty()

    override fun contains(element: String): Boolean = all.contains(element)

    override fun iterator(): Iterator<String> = all.iterator()

    override fun containsAll(elements: Collection<String>): Boolean = all.containsAll(elements)

    override fun get(index: Int): String = all[index]

    override fun indexOf(element: String): Int = all.indexOf(element)

    override fun lastIndexOf(element: String): Int = all.lastIndexOf(element)

    override fun listIterator(): ListIterator<String> = all.listIterator()

    override fun listIterator(index: Int): ListIterator<String> = all.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int): List<String> = all.subList(fromIndex, toIndex)
}

// LIGHT_ELEMENTS_NO_DECLARATION: MyInlineClass.class[add;add;addAll;addAll;clear;constructor-impl;contains-impl;containsAll-impl;equals-impl;equals-impl0;get-impl;getSize-impl;hashCode-impl;indexOf-impl;isEmpty-impl;iterator-impl;lastIndexOf-impl;listIterator-impl;listIterator-impl;remove;remove;removeAll;retainAll;set;subList-impl;toArray;toArray;toString-impl]
