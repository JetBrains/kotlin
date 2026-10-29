// STDLIB_JDK8
// JDK_KIND: FULL_JDK_21
// JVM_TARGET: 21
package test

abstract class CList<Elem> : List<Elem>

abstract class SList : List<Int>

// LIGHT_ELEMENTS_NO_DECLARATION: CList.class[add;add;addAll;addAll;addFirst;addLast;clear;getSize;listIterator;listIterator;remove;remove;removeAll;removeFirst;removeLast;replaceAll;retainAll;set;size;sort;subList;toArray;toArray], SList.class[add;add;addAll;addAll;addFirst;addLast;clear;contains;contains;getSize;indexOf;indexOf;lastIndexOf;lastIndexOf;listIterator;listIterator;remove;remove;removeAll;removeFirst;removeLast;replaceAll;retainAll;set;size;sort;subList;toArray;toArray]
