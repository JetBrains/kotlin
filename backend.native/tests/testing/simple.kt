import org.junit.*


@Test
fun simple() {}

class SimpleTestA{
    companion object {
        @BeforeClass
        fun beforeClass() {
            println("before class SimpleTestA")
        }

        @AfterClass
        fun afterClass() {
            println("after class SimpleTestA")
        }
    }
    @Before
    fun before() {
        println("SimpleTestA::before")
    }

    @After
    fun after() {
        println("SimpleTestA::after")
    }

    @Test
    fun a() {
        println("SimpleTestA::a")
    }

    @Test
    fun b() {
        println("SimpleTestA::b")
    }
}

class SimpleTestB{
    companion object {
        @BeforeClass
        fun beforeClass() {
            println("before class SimpleTestB")
        }

        @AfterClass
        fun afterClass() {
            println("after class SimpleTestB")
        }
    }

    @Before
    fun before() {
        println("SimpleTestB::before")
    }

    @After
    fun after() {
        println("SimpleTestB::after")
    }

    @Test
    fun a() {
        println("SimpleTestB::a")
    }

    @Test
    fun b() {
        println("SimpleTestB::b")
    }

}


// ---- This should be generated...
private val test = {
    val objA = SimpleTestA()

    val objB = SimpleTestB()

    TestRunner.register(listOf(
            TestSuite(
                    beforeClass = { SimpleTestA.beforeClass() },
                    afterClass = { SimpleTestA.afterClass() },
                    before = { objA.before() },
                    after = { objA.after() },
                    funcs = arrayOf({ objA.a() }, { objA.b() })),
            TestSuite(
                    beforeClass = { SimpleTestB.beforeClass() },
                    afterClass = { SimpleTestB.afterClass() },
                    before = { objB.before() },
                    after = { objB.after() },
                    funcs = arrayOf({ objB.a() }, { objB.b() }))

    ))
}()

