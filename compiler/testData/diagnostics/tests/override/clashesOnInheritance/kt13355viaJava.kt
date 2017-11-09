// FILE: K.kt
abstract class ATest1 : TestNN.JNullVsNotNull()

abstract <!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class ATest2<!> : TestNN.JUnknownImpl(), TestNN.JNotNull

abstract class ATest3 : TestNN.JUnknownVsNotNull()

class CTest1 : TestNN.JNullVsNotNull()

<!RETURN_TYPE_MISMATCH_ON_INHERITANCE!>class CTest2<!> : TestNN.JUnknownImpl(), TestNN.JNotNull

class CTest3 : TestNN.JUnknownVsNotNull()

// FILE: TestNN.java
import org.jetbrains.annotations.*;

public class TestNN {
    public interface JNull {
        @Nullable Object foo();
    }

    public interface JNotNull {
        @NotNull Object foo();
    }

    public static class JNullVsNotNull implements JNull, JNotNull {
        public Object foo() {
            return this;
        }
    }

    public static class JNullBase {
        @Nullable public Object foo() {
            return null;
        }
    }

    public static class JUnknownImpl extends JNullBase {
        public Object foo() {
            return this;
        }
    }

    public static class JUnknownVsNotNull extends JUnknownImpl implements JNotNull {
    }
}

