// FIR_IDENTICAL
// ISSUE: KT-60581
// WITH_STDLIB

// ---------------------- AssertJ declarations --------------------------
// FILE: AbstractAssert.java
public abstract class AbstractAssert<SELF extends AbstractAssert<SELF, ACTUAL>, ACTUAL> {}

// FILE: EnumerableAssert.java
public interface EnumerableAssert<SELF extends EnumerableAssert<SELF, ELEMENT>, ELEMENT> {}

// FILE: ObjectEnumerableAssert.java
public interface ObjectEnumerableAssert<SELF extends ObjectEnumerableAssert<SELF, ELEMENT>, ELEMENT>
extends EnumerableAssert<SELF, ELEMENT> {}

// FILE: IndexedObjectEnumerableAssert.java
public interface IndexedObjectEnumerableAssert<SELF extends IndexedObjectEnumerableAssert<SELF, ELEMENT>, ELEMENT>
extends ObjectEnumerableAssert<SELF, ELEMENT> {}

// FILE: AbstractIterableAssert.java
public abstract class AbstractIterableAssert<
        SELF extends AbstractIterableAssert<SELF, ACTUAL, ELEMENT, ELEMENT_ASSERT>,
ACTUAL extends Iterable<? extends ELEMENT>,
ELEMENT,
ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>>
extends AbstractAssert<SELF, ACTUAL> implements ObjectEnumerableAssert<SELF, ELEMENT> {}

// FILE: AbstractListAssert.java
public abstract class AbstractListAssert<
        SELF extends AbstractListAssert<SELF, ACTUAL, ELEMENT, ELEMENT_ASSERT>,
ACTUAL extends List<? extends ELEMENT>,
ELEMENT,
ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>>
extends AbstractIterableAssert<SELF, ACTUAL, ELEMENT, ELEMENT_ASSERT>
implements IndexedObjectEnumerableAssert<SELF, ELEMENT> {
    SELF isNotEmpty() {
        return null;
    }
}

// FILE: ListAssert.java
public class ListAssert<ELEMENT> extends AbstractListAssert<ListAssert<ELEMENT>, List<? extends ELEMENT>, ELEMENT, ObjectAssert<ELEMENT>> {}

// FILE: AbstractCharSequenceAssert.java
public abstract class AbstractCharSequenceAssert<SELF extends AbstractCharSequenceAssert<SELF, ACTUAL>, ACTUAL extends CharSequence>
extends AbstractAssert<SELF, ACTUAL> implements EnumerableAssert<SELF, Character> {}

// FILE: AbstractStringAssert.java
public class AbstractStringAssert<SELF extends AbstractStringAssert<SELF>> extends AbstractCharSequenceAssert<SELF, String> {
    public SELF isEqualTo(String expected) {
        return null;
    }
}

// FILE: StringAssert.java
public class StringAssert extends AbstractStringAssert<StringAssert> {}

// FILE: test/Assertions.java

import java.util.List;

public class Assertions {
    public static <ELEMENT> ListAssert<ELEMENT> assertThat(java.util.List<? extends ELEMENT> actual) {
        return null;
    }

    public static AbstractStringAssert<?> assertThat(String actual) {
        return null;
    }
}

// FILE: foo.kt

fun test() {
    val sessionIds = listOf("")
    val directSessionIds = listOf("")
    if (true) {
        if (true) {
            Assertions.assertThat(sessionIds[0])
        } else {
            Assertions.assertThat(directSessionIds)
        }
    } else {
        Assertions.assertThat(sessionIds)
    }
}
