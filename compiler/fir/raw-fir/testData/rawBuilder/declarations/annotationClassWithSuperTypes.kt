open class Base(x: Int)
interface I

annotation class NoSuperTypes
annotation class SuperTypeCall : Base(1)
annotation class SuperTypeWithoutCall : Base
annotation class SuperInterface : I
annotation class SuperInterfaceAndSuperTypeCall : I, Base(2)
annotation class AnySuperTypeCall : Any()
annotation class SuperTypeCallWithPrimaryConstructor(val s: String) : Base(3)
