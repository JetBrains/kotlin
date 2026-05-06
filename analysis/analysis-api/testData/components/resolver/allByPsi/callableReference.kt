package pack

import kotlin.reflect.KFunction1

open class MyClass {
   fun memberFunction(): Int = 1
   val memberProperty: Int = 2
}

class Child : MyClass()

open class ClassWithType<T> {
   fun functionWithType(): T? = null
   var propertyWithType: T? = null

   inner class InnerClass
   inner class InnerClassWithType<D>
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

fun classReferenceWithTypeArgument() {
   ClassWithType<String>::functionWithType.invoke(ClassWithType())
   pack.ClassWithType<Int>::propertyWithType.invoke(ClassWithType())
   pack.ClassWithType<Int>::InnerClass.invoke(ClassWithType())

   pack.ClassWithType<Long>::functionWithType
   ClassWithType<Boolean>::propertyWithType
   ClassWithType<Boolean>::InnerClass

   val e: KFunction1<ClassWithType<Boolean>, ClassWithType<Boolean>.InnerClassWithType<Int>> =
      ClassWithType<Boolean>::InnerClassWithType
}
