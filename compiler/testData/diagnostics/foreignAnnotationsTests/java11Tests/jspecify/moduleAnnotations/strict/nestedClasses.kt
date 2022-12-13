// FIR_IDENTICAL
// ALLOW_KOTLIN_PACKAGE
// JSPECIFY_STATE: strict

// FILE: sandbox/module-info.java
import org.jspecify.annotations.NullMarked;

@NullMarked
module sandbox {
    requires java9_annotations;
    exports test;
}

// FILE: sandbox/test/Test.java
package test;

public class Test {
    public static class Test2 {
        public Integer foo(Integer x) { return 1; }
        public static class Test4 {
            public Integer foo(Integer x) { return 1; }
        }
        public class Test5 {
            public Integer foo(Integer x) { return 1; }
        }
    }
    public class Test3 {
        public Integer foo(Integer x) { return 1; }
        public class Test5 {
            public Integer foo(Integer x) { return 1; }
        }
    }
}

// FILE: main.kt
import test.Test

fun main(x: Test) {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x.Test3().foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x.Test3().Test5().foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>Test.Test2().foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>Test.Test2().Test5().foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!>
}