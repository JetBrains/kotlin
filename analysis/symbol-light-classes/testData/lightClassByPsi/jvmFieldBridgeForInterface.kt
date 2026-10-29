// LIBRARY_PLATFORMS: JVM

open class ValBase(
    @JvmField val foo: String
)

open class ValBaseGeneric<T>(
    @JvmField val foo: T
)

open class VarBase(
    @JvmField var foo: String
)

interface I1 {
    val foo: Any
}

interface I2 {
    val foo: CharSequence
}

interface I2Generic<T : CharSequence> : I1 {
    override val foo: T
}

interface I3Val {
    val foo: String
}

interface I3Var {
    var foo: String
}

class C1 : ValBase("OK"), I1, I2
class C2 : ValBase("OK"), I1, I2, I3Val
class C3 : VarBase("Fail"), I3Var

open class Mid<T : CharSequence>(foo: T) : ValBaseGeneric<T>(foo), I2Generic<T>
class C4 : Mid<String>("OK")
