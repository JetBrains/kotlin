// KT-668 Failed to resolve generic parameter
open class A()
open class B() : A() {
  fun b(): B = B()
}


class C() {
  fun <T> a(x: (T)->T, y: T): T {
    return x(x(y))
  }

  val x: B = a({it.b()}, B())
}
