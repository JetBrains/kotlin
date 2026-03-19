// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-39491

// KT-39491: "Recursive call in a lazy value under LockBasedStorageManager" when accessing constructors
// of @Serializable classes via a SyntheticResolveExtension compiler plugin.
// In K1, calling ClassDescriptor#getConstructors from generateSyntheticMethods on a @Serializable
// class caused a recursive call crash. This test verifies FIR handles such classes correctly.

annotation class Serializable

@Serializable
class MySerializableClass(val id: Int, val name: String) {
    constructor(id: Int) : this(id, "default")
    constructor() : this(0, "empty")
}

@Serializable
data class SerializableData(val x: Double, val y: Double)

fun test() {
    val obj1 = MySerializableClass(1, "hello")
    val obj2 = MySerializableClass(2)
    val obj3 = MySerializableClass()
    val data = SerializableData(1.0, 2.0)
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, data, functionDeclaration, integerLiteral, localProperty,
primaryConstructor, propertyDeclaration, secondaryConstructor, stringLiteral */
