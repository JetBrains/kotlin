package kotlin.test.tests

import kotlin.test.*
import konan.test.*


@Ignore
class IgnoredClass {
    @Ignore object IgnoredObject {
        @Test fun test() { throw AssertionError("Ignored test") }
    }

    @Test fun test() { throw AssertionError("Ignored test") }
}

@Ignore
object IgnoredObject {
    @Test fun test() { throw AssertionError("Ignored test") }
}

class A {
    companion object {
        @BeforeEach fun before() { println("before (A.companion)") }
        @AfterEach fun after() { println("after (A.companion)") }
        @Test fun test1() { println("test1 (A.companion)") }

        @BeforeClass fun beforeClass() { println("beforeClass (A.companion)") }
        @AfterClass fun afterClass() { println("afterClass (A.companion)") }
    }

    @BeforeEach fun before() { println("before (A)") }
    @AfterEach fun after() { println("after (A)") }
    @Test fun test1() { println("test1 (A)") }
    @Ignore @Test fun ignoredTest() { throw AssertionError("Ignored test") }

    @BeforeClass fun beforeClass() { println("beforeClass (A)") }
    @AfterClass fun afterClass() { println("afterClass (A)") }

    object O {
        @BeforeEach fun before() { println("before (A.object)") }
        @AfterEach fun after() { println("after (A.object)") }
        @Test fun test1() { println("test1 (A.object)") }
        @Ignore @Test fun ignoredTest() { throw AssertionError("Ignored test") }

        @BeforeClass fun beforeClass() { println("beforeClass (A.object)") }
        @AfterClass fun afterClass() { println("afterClass (A.object)") }
    }
}

object O {
    @BeforeEach fun before() { println("before (object)") }
    @AfterEach fun after() { println("after (object)") }
    @Test fun test1() { println("test1 (object)") }
    @Ignore @Test fun ignoredTest() { throw AssertionError("Ignored test") }

    @BeforeClass fun beforeClass() { println("beforeClass (object)") }
    @AfterClass fun afterClass() { println("afterClass (object)") }
}

@BeforeEach fun before() { println("before (file)") }
@AfterEach fun after() { println("after (file)") }
@Test fun test1() { println("test1 (file)") }
@Ignore @Test fun ignoredTest() { throw AssertionError("Ignored test") }

@BeforeClass fun beforeClass() { println("beforeClass (file)") }
@AfterClass fun afterClass() { println("afterClass (file)") }
