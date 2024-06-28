// FULL_JDK
// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN
// ISSUE: KT-65184

// FILE: A.java

public class A<T> {

}

// FILE: B.java

public class B<T> extends A<T> {

}

// FILE: box.kt

import java.util.LinkedList
import java.util.Queue

fun bar(b: A<String>) {}

fun func(p: A<B<String>>) {
}

class X {
    fun bar(b: Queue<String>) {}
}

fun test(x : X) {
    x.bar(<!TYPE_MISMATCH!>LinkedList<String?>()<!>)
    bar(<!TYPE_MISMATCH!>B<String?>()<!>)
    func(A<B<String>>())
    func(<!TYPE_MISMATCH!>A<B<String?>>()<!>)
    func(<!TYPE_MISMATCH!>A<B<String?>?>()<!>)
    func(<!TYPE_MISMATCH!>A<B<String>?>()<!>)
}

class C {
    fun bar(b: Queue<String>) {}
}

fun test(c: C, jj: LinkedList<String?>) {
    c.bar(<!TYPE_MISMATCH!>jj<!>)
}
