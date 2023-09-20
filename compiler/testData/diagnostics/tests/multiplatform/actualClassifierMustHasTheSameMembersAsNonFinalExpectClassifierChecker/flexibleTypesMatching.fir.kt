// MODULE: m1-common
// FILE: common.kt

expect open class Base {
    <!INCOMPATIBLE_MATCHING{JVM}!>open fun foo(): MutableList<String><!>
}

<!INCOMPATIBLE_MATCHING{JVM}!>expect open class Foo : Base {

}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Base = BaseJava

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class Foo<!> : Base() {
    // K1 doesn't report RETURN_TYPE_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION a diagnostic here because when it compares scopes it sees flexible type
    // K2 behavior is correct
    override fun foo(): <!RETURN_TYPE_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION!>List<String><!> {
        return super.foo()
    }
}

// FILE: BaseJava.java
import java.util.List;

public class BaseJava {
    public List<String> foo() {
        return null;
    }
}
