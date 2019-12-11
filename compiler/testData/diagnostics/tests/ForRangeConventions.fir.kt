// !CHECK_TYPE

import java.util.*;

class NotRange1() {

}

abstract class NotRange2() {
  abstract operator fun iterator() : Unit
}

abstract class ImproperIterator1 {
  abstract operator fun hasNext() : Boolean
}

abstract class NotRange3() {
  abstract operator fun iterator() : ImproperIterator1
}

abstract class ImproperIterator2 {
  abstract operator fun next() : Boolean
}

abstract class NotRange4() {
  abstract operator fun iterator() : ImproperIterator2
}

abstract class ImproperIterator3 {
  abstract operator fun hasNext() : Int
  abstract operator fun next() : Int
}

abstract class NotRange5() {
  abstract operator fun iterator() : ImproperIterator3
}

abstract class AmbiguousHasNextIterator {
  abstract operator fun hasNext() : Boolean
  val hasNext : Boolean get() = false
  abstract operator fun next() : Int
}

abstract class NotRange6() {
  abstract operator fun iterator() : AmbiguousHasNextIterator
}

abstract class ImproperIterator4 {
  val hasNext : Int get() = 1
  abstract operator fun next() : Int
}

abstract class NotRange7() {
  abstract operator fun iterator() : ImproperIterator3
}

abstract class GoodIterator {
  abstract operator fun hasNext() : Boolean
  abstract operator fun next() : Int
}

abstract class Range0() {
  abstract operator fun iterator() : GoodIterator
}

abstract class Range1() {
  abstract operator fun iterator() : Iterator<Int>
}

abstract class ImproperIterator5 {
    abstract val String.hasNext : Boolean
    abstract operator fun next() : Int
}

abstract class NotRange8() {
    abstract operator fun iterator() : ImproperIterator5
}


fun test(notRange1: NotRange1, notRange2: NotRange2, notRange3: NotRange3, notRange4: NotRange4, notRange5: NotRange5, notRange6: NotRange6, notRange7: NotRange7, notRange8: NotRange8, range0: Range0, range1: Range1) {
  <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for (i in notRange1)<!>;
  <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for (i in notRange2)<!>;
  <!UNRESOLVED_REFERENCE!>for (i in notRange3)<!>;
  <!UNRESOLVED_REFERENCE!>for (i in notRange4)<!>;
  for (i in notRange5);
  for (i in notRange6);
  for (i in notRange7);
  <!UNRESOLVED_REFERENCE!>for (i in notRange8)<!>;
  for (i in range0);
  for (i in range1);

  for (i in (checkSubtype<List<Int>>(ArrayList<Int>())));
}