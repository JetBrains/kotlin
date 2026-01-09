// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-55548
// ISSUE: KT-9182

// FILE: JavaUtils.java
import org.jetbrains.annotations.NotNull;

public class JavaUtils {
    public static String foo1(int b) { return null; } // (1)
    public static int foo1(Integer b) { return 0; } // (2)

    public static String foo2(int b) { return null; } // (1)
    public static int foo2(@NotNull Integer b) { return 0; } // (2)

    public static String foo3(long b) { return null; } // (1)
    public static int foo3(int b) { return 0; } // (2)

    public static String foo4(long b) { return null; } // (1)
    public static int foo4(Integer b) { return 0; } // (2)

    public static String foo5(long b) { return null; } // (1)
    public static int foo5(@NotNull Integer b) { return 0; } // (2)
}

// FILE: main.kt
fun main() {
    <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String..kotlin.String?)")!>JavaUtils.foo1(1)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: foo2, [/JavaUtils.foo2, /JavaUtils.foo2]")!>JavaUtils.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>(1)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>JavaUtils.foo3(1)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: foo4, [/JavaUtils.foo4, /JavaUtils.foo4]")!>JavaUtils.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo4<!>(1)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>JavaUtils.foo5(1)<!>
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, integerLiteral, javaFunction */
