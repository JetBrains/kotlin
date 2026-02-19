fun @receiver:FunctionReceiverAnnotation(1) @ReceiverTypeAnnotation(2) List<@ReceiverNestedTypeAnnotation(3) String>.function() {

}

val @receiver:PropertyReceiverAnnotation(4) @ReceiverTypeAnnotation(5) List<@ReceiverNestedTypeAnnotation(6) String>.property
    get() = 0

context(List<@ContextReceiverAnnotation(7) Int>, List<@ContextReceiverAnnotation String>)
fun functionWithContextReceivers() {}

context(List<@ContextReceiverAnnotation(8) Long>, List<@ContextReceiverAnnotation Boolean>)
val propertyWithContextReceivers get() = 0

context(List<@ContextReceiverAnnotation(9) Short>, List<@ContextReceiverAnnotation UInt>)
class MyClass

context(List<@ContextReceiverAnnotation(10) Short>, List<@ContextReceiverAnnotation UInt>)
class MyClassWithExplicitConstructor constructor() {
    constructor(i: Int) : this()
}
