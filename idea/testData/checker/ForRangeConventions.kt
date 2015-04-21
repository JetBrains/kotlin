import java.util.*;

fun <T> checkSubtype(t: T) = t

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

fun test(notRange1: NotRange1, notRange2: NotRange2, notRange3: NotRange3, notRange4: NotRange4, notRange5: NotRange5, notRange6: NotRange6, notRange7: NotRange7, range0: Range0, range1: Range1) {
  for (i in <error>notRange1</error>);
  for (i in <error>notRange2</error>);
  for (i in <error>notRange3</error>);
  for (i in <error>notRange4</error>);
  for (i in <error>notRange5</error>);
  for (i in notRange6);
  for (i in <error>notRange7</error>);
  for (i in range0);
  for (i in range1);

  for (i in (checkSubtype<List<Int>>(ArrayList<Int>())));
}

