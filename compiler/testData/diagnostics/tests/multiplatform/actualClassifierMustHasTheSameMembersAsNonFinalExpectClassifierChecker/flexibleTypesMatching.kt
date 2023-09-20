// MODULE: m1-common
// FILE: common.kt

expect open class Base {
    open fun foo(): MutableList<String>
}

expect open class Foo : Base {

}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Base = BaseJava

actual open class Foo : Base() {
    // K1 doesn't report RETURN_TYPE_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION a diagnostic here because when it compares scopes it sees flexible type
    // K2 behavior is correct
    override fun foo(): List<String> {
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
