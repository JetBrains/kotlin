val a = object: T {}
open class C
interface T

annotation class Ann: C()
annotation class Ann2: T
annotation class Ann3: T by a
annotation class Ann4: C(), T