//  Ignore reason: KT-57619
import <!UNRESOLVED_IMPORT!>my<!>.println

enum class Order {
    FIRST,
    SECOND,
    THIRD
}

enum class Planet(val m: Double, internal val r: Double) {
    MERCURY(1.0, 2.0) {
        override fun sayHello() {
            <!UNRESOLVED_REFERENCE!>println<!>("Hello!!!")
        }
    },
    VENERA(3.0, 4.0) {
        override fun sayHello() {
            <!UNRESOLVED_REFERENCE!>println<!>("Ola!!!")
        }
    },
    EARTH(5.0, 6.0) {
        override fun sayHello() {
            <!UNRESOLVED_REFERENCE!>println<!>("Privet!!!")
        }
    };

    val g: Double = <!UNINITIALIZED_ENUM_COMPANION!>G<!> * m / (r * r)

    abstract fun sayHello()

    companion object {
        const val G = 6.67e-11
    }
}
