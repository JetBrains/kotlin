//FILE:_03_collections/CollectionTest.java
package _03_collections;

import java.util.List;

public class CollectionTest {
    public static void add(List<Integer> ints) {
        ints.add(5);
    }
}

//FILE:n.kt
package _03_collections

import java.util.ArrayList

fun test() {
  val <!UNUSED_VARIABLE!>c<!> = CollectionTest()
  CollectionTest.add(ArrayList())
}
