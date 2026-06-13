// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-22961

// KT-22961: Overload resolution ambiguity when calling overloaded function on parameterized class instantiated with Any

class Foo<T> {
    fun bar(a: T) {}
    fun bar(a: String) {}
}

fun test(f: Foo<Any>) {
    f.bar("")
}

class OverloadingTestClass<in F> {
    fun doSomething(genericParam: F) {}
    fun doSomething(param: String) {}
}

fun callTest() {
    val overloadingTestClass = OverloadingTestClass<Any>()
    val testString = "testString"
    overloadingTestClass.doSomething(testString)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, in, localProperty, nullableType, propertyDeclaration,
stringLiteral, typeParameter */
