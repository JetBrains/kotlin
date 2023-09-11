open class C1

interface I1

external open class EC1

external class EC2 : C1

external class EC3 : I1, C1

external interface EI1 : I1

interface I2 : EI1

class C3 : EI1

class C4 : EI1, EC1()

object O1 : EC1()

val x1: Any = object : EI1 {}
val x2: Any = object : EC1() {}
