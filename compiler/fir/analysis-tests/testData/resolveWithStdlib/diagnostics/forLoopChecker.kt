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
    abstract <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun hasNext() : Int
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

abstract class ImproperIterator6 {
    abstract fun hasNext() : Boolean
    abstract fun next() : Int
}

abstract class NotRange8() {
    abstract operator fun iterator() : ImproperIterator5
}

abstract class NotRange9() {
    abstract fun iterator(): ImproperIterator6
}


fun test(
    notRange1: NotRange1,
    notRange2: NotRange2,
    notRange3: NotRange3,
    notRange4: NotRange4,
    notRange5: NotRange5,
    notRange6: NotRange6,
    notRange7: NotRange7,
    notRange8: NotRange8,
    notRange9: NotRange9,
    range0: Range0,
    range1: Range1
) {
    for (i in <!ITERATOR_MISSING!>notRange1<!>);
    for (i in <!HAS_NEXT_MISSING, NEXT_MISSING!>notRange2<!>);
    for (i in <!NEXT_MISSING!>notRange3<!>);
    for (i in <!HAS_NEXT_MISSING!>notRange4<!>);
    for (i in <!CONDITION_TYPE_MISMATCH!>notRange5<!>);
    for (i in notRange6);
    for (i in <!CONDITION_TYPE_MISMATCH!>notRange7<!>);
    for (i in <!HAS_NEXT_MISSING!>notRange8<!>);
    for (i in <!OPERATOR_MODIFIER_REQUIRED!>notRange9<!>);
    for (i in range0);
    for (i in range1);
}
