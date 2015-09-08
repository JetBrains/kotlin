annotation class Base(val x: Int)

annotation class UseBase(val b: Base = Base(0))

@UseBase class My
