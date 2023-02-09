// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect enum class Foo {
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>ENTRY<!>
}<!>

expect enum class _TimeUnit

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual typealias Foo = FooImpl

actual typealias _TimeUnit = java.util.concurrent.TimeUnit

// FILE: FooImpl.java

public enum FooImpl {
    ENTRY("OK") {
        @Override
        public String getResult() {
            return value;
        }
    };

    protected final String value;

    public FooImpl(String value) {
        this.value = value;
    }

    public abstract String getResult();
}
