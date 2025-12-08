// TARGET_BACKEND: JVM
// ISSUE: KT-81948

// FILE: JavaUtils.java
import org.jetbrains.annotations.Nullable;

public class JavaUtils {
    @Nullable
    public static <T> T javaFunc() {
        return (T) "OK";
    }
}

// FILE: TestClass.kt
class TestClass {
    private var str: String? = null

    fun testFun(): String? {
        str = "".let {
            JavaUtils.javaFunc()
        }!!
        return str
    }
}

fun box(): String = TestClass().testFun()!!

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, classDeclaration, functionDeclaration, javaFunction, lambdaLiteral,
nullableType, propertyDeclaration, stringLiteral */
