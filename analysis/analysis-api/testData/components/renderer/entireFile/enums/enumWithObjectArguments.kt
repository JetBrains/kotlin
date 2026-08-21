interface Some

object O1 : Some

object O2 : Some

enum class SomeEnum(val x: Some) {
    FIRST(O1) {
        override fun check(y: Some): Boolean = true
    },
    SECOND(O2)  {
        override fun check(y: Some): Boolean = y == O2
    };

    abstract fun check(y: Some): Boolean
}