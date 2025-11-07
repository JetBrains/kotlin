// WITH_STDLIB
// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// Public property with explicit type
val a: String = "A"
// Public property with implicit type
val b = "B"
// Property with overriden getter and implicit type.
fun foo() = "C"
val c get() = foo()
// Property initializer with control statement.
val d = true
val e = if(d) "True" else "False"
// Property with backing field use in setter.
var f: Int
        set(value) {
            if (value >= 0) {
                // 'field' references the actual backing storage
                field = value
            }
        }
// Property with backing field use in getter.
val g: Int
    get() {
        return g + 1
    }
// Delegated property with implicit type.
val h by lazy {
    "H"
}
const val i = 123
val j = object {
    fun foo() = "foo"
    fun bar(): String = "bar"
}
val k = fun(foo: String): String {
    return "foo: $foo"
}
val l = fun(foo: String): String { return "Foo: $foo" }
/* GENERATED_FIR_TAGS: propertyDeclaration, stringLiteral */
