import my.println

enum class Order {
    FIRST,
    SECOND,
    THIRD
}

enum class Planet(val m: Double, internal val r: Double) {
    MERCURY(1.0, 2.0) {
        override fun sayHello() {
            println("Hello!!!")
        }
    },
    VENERA(3.0, 4.0) {
        override fun sayHello() {
            println("Ola!!!")
        }
    },
    EARTH(5.0, 6.0) {
        override fun sayHello() {
            println("Privet!!!")
        }
    };

    val g: Double = G * m / (r * r)

    abstract fun sayHello()

    companion object {
        const val G = 6.67e-11
    }
}

enum class PseudoInsn(val signature: String = "()V") {
    FIX_STACK_BEFORE_JUMP,
    FAKE_ALWAYS_TRUE_IFEQ("()I"),
    FAKE_ALWAYS_FALSE_IFEQ("()I"),
    SAVE_STACK_BEFORE_TRY,
    RESTORE_STACK_IN_TRY_CATCH,
    STORE_NOT_NULL,
    AS_NOT_NULL("(Ljava/lang/Object;)Ljava/lang/Object;")
    ;

    fun emit() {}
}