package p

interface I1
interface I2
interface I3

interface KotlinTrait<T1, T2>

open class KotlinInheritor1<T> : KotlinTrait<T, I2>

class KotlinInheritor2 : KotlinInheritor1<I1>()

// is not suitable because type arguments do not match
class KotlinInheritor3 : KotlinInheritor1<Any>()

abstract class KotlinInheritor4<T, V> : KotlinTrait<T, V>

// is not suitable because type arguments do not match
class KotlinInheritor5 : KotlinTrait<Char, I2>

class KotlinInheritor6<T1, T2, T3 : I3> : KotlinTrait<T1, I2>

// ALLOW_AST_ACCESS
