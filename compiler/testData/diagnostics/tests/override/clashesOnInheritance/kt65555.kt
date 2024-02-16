// FIR_IDENTICAL
// FULL_JDK
// WITH_STDLIB
// ISSUE: KT-65555

interface MyCollection<E> : Collection<E>
interface MyList<E> : MyCollection<E>, List<E>
interface MyMutableList<E> : MyList<E>, MutableList<E>