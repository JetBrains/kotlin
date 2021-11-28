// FILE: MyList.java
public interface MyList<E> extends java.util.List<E>, I {}

// FILE: main.kt

fun <R> elemAndList(r: R, t: MutableList<R>): R = TODO()

interface I
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class A<!> : Comparable<A>, I
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class B<!> : Comparable<B>, I

fun test() {
    elemAndList(A(), list(B()))
}

fun <T> list(value: T): MyList<T> = TODO()
