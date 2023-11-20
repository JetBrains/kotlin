// MODULE: m1-common
// FILE: common.kt

expect open class Base {
    open fun foo(): MutableList<String>
}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}, EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect open class Foo : Base {

}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Base = BaseJava

actual open class Foo : Base() {
    // K1 doesn't report a diagnostic here because when it compares scopes it sees flexible type
    // K2 will likely report a diagnostic here
    // I don't think we can fix this 'K1 green -> K2 red'. It must be a rare case anyway.
    override fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>(): List<String> {
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
