package pack

open class ClassWithType<T> {
   inner class InnerClassWithType<D>
}

fun classReferenceWithTypeArgument() {
    ClassWithType<Boolean>::InnerClassWithType<String>
    //                                               ^ the type argument is prohibited here
}
