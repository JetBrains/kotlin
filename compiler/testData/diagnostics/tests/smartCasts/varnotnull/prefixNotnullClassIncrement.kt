// RUN_PIPELINE_TILL: BACKEND
class MyClass

operator fun MyClass.inc(): MyClass { return null!! }

public fun box() {
    var i : MyClass?
    i = MyClass()
    // Type of j should be inferred as MyClass?
    var j = ++i
    // j is null so call is unsafe
    j.hashCode()
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
incrementDecrementExpression, localProperty, nullableType, operator, propertyDeclaration, smartcast */
