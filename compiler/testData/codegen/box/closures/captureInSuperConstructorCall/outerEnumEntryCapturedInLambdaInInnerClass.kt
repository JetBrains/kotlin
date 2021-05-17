abstract class Base(val fn: () -> Test)

enum class Test(val ok: String) {
    TEST("OK") {
        inner class Inner : Base({ TEST })

        override val base: Base
            get() = Inner()
    };

    abstract val base: Base
}

fun box() = Test.TEST.base.fn().ok