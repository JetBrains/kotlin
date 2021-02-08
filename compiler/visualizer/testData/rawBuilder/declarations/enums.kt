// FIR_IGNORE
import my.println

enum class Order {
    FIRST,
    SECOND,
    THIRD
}

enum class Planet(val m: Double, internal val r: Double) {
//         constructor Planet(Double, Double)
//         │Double
//         ││    Double
//         ││    │
    MERCURY(1.0, 2.0) {
        override fun sayHello() {
//          fun io/println(Any?): Unit
//          │
            println("Hello!!!")
        }
    },
//        constructor Planet(Double, Double)
//        │Double
//        ││    Double
//        ││    │
    VENERA(3.0, 4.0) {
        override fun sayHello() {
//          fun io/println(Any?): Unit
//          │
            println("Ola!!!")
        }
    },
//       constructor Planet(Double, Double)
//       │Double
//       ││    Double
//       ││    │
    EARTH(5.0, 6.0) {
        override fun sayHello() {
//          fun io/println(Any?): Unit
//          │
            println("Privet!!!")
        }
    };

//                  val (Planet.Companion).G: Double
//                  │ fun (Double).times(Double): Double
//                  │ │ val (Planet).m: Double
//                  │ │ │ fun (Double).div(Double): Double
//                  │ │ │ │  val (Planet).r: Double
//                  │ │ │ │  │ fun (Double).times(Double): Double
//      Double      │ │ │ │  │ │ val (Planet).r: Double
//      │           │ │ │ │  │ │ │
    val g: Double = G * m / (r * r)

    abstract fun sayHello()

    companion object {
//                Double
//                │   Double
//                │   │
        const val G = 6.67e-11
    }
}

enum class PseudoInsn(val signature: String = "()V") {
    FIX_STACK_BEFORE_JUMP,
//                       constructor PseudoInsn(String = ...)
//                       │
    FAKE_ALWAYS_TRUE_IFEQ("()I"),
//                        constructor PseudoInsn(String = ...)
//                        │
    FAKE_ALWAYS_FALSE_IFEQ("()I"),
    SAVE_STACK_BEFORE_TRY,
    RESTORE_STACK_IN_TRY_CATCH,
    STORE_NOT_NULL,
//             constructor PseudoInsn(String = ...)
//             │
    AS_NOT_NULL("(Ljava/lang/Object;)Ljava/lang/Object;")
    ;

    fun emit() {}
}
