// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNUSED_EXPRESSION
// Issue: KT-35844

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

// FILE: Assertions.java
public class Assertions {
    public static <ELEMENT> ListAssert<ELEMENT> assertThat(java.util.List<? extends ELEMENT> actual) {
        return null;
    }

    public static AbstractStringAssert<?> assertThat(String actual) {
        return null;
    }
}
// ---------------------- AssertJ declarations end --------------------------

// FILE: test.kt
fun test() {
    val assertion = when {
        true -> Assertions.assertThat(listOf("foo")).isNotEmpty
        else -> Assertions.assertThat("bar").isEqualTo("bar")
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("AbstractAssert<*, out kotlin.Any..kotlin.Any?!> & EnumerableAssert<*, out kotlin.Comparable<kotlin.String & kotlin.Char> & java.io.Serializable..kotlin.Comparable<kotlin.String & kotlin.Char>? & java.io.Serializable?>..AbstractAssert<*, out kotlin.Any..kotlin.Any?!>? & EnumerableAssert<*, out kotlin.Comparable<kotlin.String & kotlin.Char> & java.io.Serializable..kotlin.Comparable<kotlin.String & kotlin.Char>? & java.io.Serializable?>?")!>assertion<!>
}
