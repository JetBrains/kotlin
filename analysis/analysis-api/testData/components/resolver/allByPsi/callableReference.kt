package pack

open class MyClass {
    fun memberFunction(): Int = 1
    val memberProperty: Int = 2
}

class Child : MyClass()

open class ClassWithType<T> {
    fun functionWithType(): T? = null
    var propertyWithType: T? = null
}

class ChildWithType : ClassWithType<MyClass>()

fun topLevelFunction(i: Int) {}

var topLevelVariable = 1

fun MyClass.extensionFunction(): String = "str"
val MyClass.extensionProperty: String get() = "rts"

lateinit var lateinitVar: MyClass

fun usage() {
    MyClass::memberFunction.invoke(MyClass())
    MyClass::memberProperty.name

    Child::memberFunction.invoke(Child())
    Child::memberProperty.name

    val f = pack.MyClass::extensionFunction
    val p = MyClass::extensionProperty

    val cF = Child::extensionFunction
    val cP = pack.Child::extensionProperty

    ChildWithType::functionWithType.name
    ChildWithType::propertyWithType.invoke(ChildWithType())

    ::topLevelFunction
    ::topLevelVariable

    ::lateinitVar.isInitialized
}

fun <T : Number> typeClass(classWithType: ClassWithType<T>) {
    classWithType::functionWithType
    classWithType::propertyWithType

    val t: ClassWithType<Int>? = null
    t!!::functionWithType
    t!!::propertyWithType
}