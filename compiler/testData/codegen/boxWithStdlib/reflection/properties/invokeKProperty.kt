import kotlin.reflect.declaredMemberProperties

class A(val foo: String)

fun box(): String {
    return (A::class.declaredMemberProperties.single()).invoke(A("OK")) as String
}
