// STDLIB_JDK8
// FULL_JDK
package test

abstract class StringList : AbstractList<String>()

abstract class IntList : AbstractList<Int>()

abstract class NullableIntList : AbstractList<Int?>()

abstract class GenericList<E> : AbstractList<E>()

abstract class BelowGenericList : GenericList<String>()

abstract class BelowStringList : StringList()

abstract class BoundedList<E : CharSequence> : AbstractList<E>()

abstract class BelowBoundedList : BoundedList<String>()

abstract class StringMutableList : AbstractMutableList<String>()

abstract class OverridingMutableList : AbstractMutableList<String>() {
    override fun removeAt(index: Int): String = ""
}

abstract class BelowStringMutableList : StringMutableList() {
    override fun removeAt(index: Int): String = ""
}

abstract class DirectList<E> : List<E>

abstract class BelowDirectList : DirectList<String>()

abstract class DirectMutableList<E> : MutableList<E>

abstract class BelowDirectMutableList : DirectMutableList<String>()

abstract class StringCollection : AbstractCollection<String>()

abstract class StringSet : AbstractSet<String>()

abstract class StringMutableSet : AbstractMutableSet<String>()

abstract class StringMutableCollection : AbstractMutableCollection<String>()

object ListObject : AbstractList<String>() {
    override val size: Int get() = 0
    override fun get(index: Int): String = ""
}

// LIGHT_ELEMENTS_NO_DECLARATION: BelowDirectList.class[contains;contains;indexOf;indexOf;lastIndexOf;lastIndexOf], BelowDirectMutableList.class[contains;contains;indexOf;indexOf;lastIndexOf;lastIndexOf;remove;remove;remove;removeAt], BelowGenericList.class[contains;contains;indexOf;indexOf;lastIndexOf;lastIndexOf], BoundedList.class[contains;contains;indexOf;indexOf;lastIndexOf;lastIndexOf], DirectList.class[add;add;addAll;addAll;clear;getSize;listIterator;listIterator;remove;remove;removeAll;replaceAll;retainAll;set;size;sort;subList;toArray;toArray], DirectMutableList.class[getSize;remove;removeAt;size;toArray;toArray], IntList.class[contains;contains;indexOf;indexOf;lastIndexOf;lastIndexOf], ListObject.class[contains;contains;indexOf;indexOf;lastIndexOf;lastIndexOf], NullableIntList.class[contains;contains;indexOf;indexOf;lastIndexOf;lastIndexOf], OverridingMutableList.class[contains;contains;indexOf;indexOf;lastIndexOf;lastIndexOf;remove;remove;remove], StringCollection.class[contains;contains;iterator], StringList.class[contains;contains;indexOf;indexOf;lastIndexOf;lastIndexOf], StringMutableCollection.class[contains;contains;remove;remove], StringMutableList.class[contains;contains;indexOf;indexOf;lastIndexOf;lastIndexOf;remove;remove;remove;removeAt], StringMutableSet.class[contains;contains;remove;remove], StringSet.class[contains;contains]
