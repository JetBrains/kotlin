// JSPECIFY_STATE: warn
// ALLOW_KOTLIN_PACKAGE

// FILE: sandbox/module-info.java
import org.jspecify.nullness.NullMarked;

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
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int..kotlin.Int?!")!>x.Test3().foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int..kotlin.Int?!")!>x.Test3().Test5().foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)<!>

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int..kotlin.Int?!")!>Test.Test2().foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int..kotlin.Int?!")!>Test.Test2().Test5().foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)<!>
}
