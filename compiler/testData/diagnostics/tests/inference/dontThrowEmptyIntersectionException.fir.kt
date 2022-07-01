// WITH_STDLIB

// FILE: Test.java
public interface Test<K> implements I<String> {}

// FILE: Test2.java
public interface Test2<K, M>

// FILE: Test3.java
public interface Test3<K> {}

// FILE: I.java
public interface I<K> {}

// FILE: main.kt
fun main(z: I<String>) {
    z <!UNCHECKED_CAST!>as Test<Test2<Int, *>><!>
    z <!UNCHECKED_CAST!>as Test<Test2<Int, <!UNRESOLVED_REFERENCE!>Foo<!>>><!>
    z as Test<<!UNRESOLVED_REFERENCE!>Foo<!>>
    z as <!UNRESOLVED_REFERENCE!>Any2<!>
    println(z)
}