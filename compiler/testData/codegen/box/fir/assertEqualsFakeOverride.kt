// TARGET_BACKEND: JVM_IR
// FILE: AbstractBlackBoxCodegenTest.java

public abstract class AbstractBlackBoxCodegenTest extends CodegenTestCase {}

// FILE: CodegenTestCase.java

public abstract class CodegenTestCase extends KotlinBaseTest<CharSequence> {}

// FILE: KotlinBaseTest.kt

abstract class KotlinBaseTest<F : CharSequence> : KtUsefulTestCase() {}

// FILE: KtUsefulTestCase.java

public abstract class KtUsefulTestCase extends TestCase {}

// FILE: TestCase.java

public abstract class TestCase {
    public static void assertEquals(int expected, int actual) {

    }
}

// FILE: test.kt

fun box(): String {
    AbstractBlackBoxCodegenTest.assertEquals(42, 42)
    return "OK"
}
