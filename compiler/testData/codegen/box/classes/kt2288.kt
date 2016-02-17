// TODO: Enable when JS backend supports Java class library
// TARGET_BACKEND: JVM
public open class Test(): java.util.RandomAccess, Cloneable, java.io.Serializable
{
        public override fun clone(): Test = Test() // Override 'clone()' with more precise type 'Test'

        public override fun toString() = "OK"
}

fun box() = Test().clone().toString()
