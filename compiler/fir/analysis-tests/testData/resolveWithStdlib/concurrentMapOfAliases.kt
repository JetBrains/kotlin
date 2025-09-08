// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
private typealias MyAlias = CharSequence

class A {
    private val foo = java.util.concurrent.ConcurrentHashMap<String, MyAlias>()

    private fun bar() {
        foo["dd"]?.baz()
    }

    private fun MyAlias.baz() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration, javaFunction,
nullableType, propertyDeclaration, safeCall, stringLiteral, typeAliasDeclaration */
