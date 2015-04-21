// !CHECK_TYPE

import java.util.*;

class NotRange1() {

}

abstract class NotRange2() {
  abstract fun iterator() : Unit
}

abstract class ImproperIterator1 {
  abstract fun hasNext() : Boolean
}

abstract class NotRange3() {
  abstract fun iterator() : ImproperIterator1
}

abstract class ImproperIterator2 {
  abstract fun next() : Boolean
}

abstract class NotRange4() {
  abstract fun iterator() : ImproperIterator2
}

abstract class ImproperIterator3 {
  abstract fun hasNext() : Int
  abstract fun next() : Int
}

abstract class NotRange5() {
  abstract fun iterator() : ImproperIterator3
}

abstract class AmbiguousHasNextIterator {
  abstract fun hasNext() : Boolean
  val hasNext : Boolean get() = false
  abstract fun next() : Int
}

abstract class NotRange6() {
  abstract fun iterator() : AmbiguousHasNextIterator
}

abstract class ImproperIterator4 {
  val hasNext : Int get() = 1
  abstract fun next() : Int
}

abstract class NotRange7() {
  abstract fun iterator() : ImproperIterator3
}

abstract class GoodIterator {
  abstract fun hasNext() : Boolean
  abstract fun next() : Int
}

abstract class Range0() {
  abstract fun iterator() : GoodIterator
}

abstract class Range1() {
  abstract fun iterator() : Iterator<Int>
}

abstract class ImproperIterator5 {
    abstract val String.hasNext : Boolean
    abstract fun next() : Int
}

abstract class NotRange8() {
    abstract fun iterator() : ImproperIterator5
}


fun test(notRange1: NotRange1, notRange2: NotRange2, notRange3: NotRange3, notRange4: NotRange4, notRange5: NotRange5, notRange6: NotRange6, notRange7: NotRange7, notRange8: NotRange8, range0: Range0, range1: Range1) {
  for (i in <!ITERATOR_MISSING!>notRange1<!>);
  for (i in <!HAS_NEXT_MISSING, NEXT_MISSING!>notRange2<!>);
  for (i in <!NEXT_MISSING!>notRange3<!>);
  for (i in <!HAS_NEXT_MISSING!>notRange4<!>);
  for (i in <!HAS_NEXT_FUNCTION_TYPE_MISMATCH!>notRange5<!>);
  for (i in notRange6);
  for (i in <!HAS_NEXT_FUNCTION_TYPE_MISMATCH!>notRange7<!>);
  for (i in <!HAS_NEXT_MISSING!>notRange8<!>);
  for (i in range0);
  for (i in range1);

  for (i in (checkSubtype<List<Int>>(ArrayList<Int>())));
}