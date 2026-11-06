// WITH_STDLIB
// DUMP_IR_AFTER_INLINE
// ISSUE: KT-91111

open class Base {
    fun foo(): String = "foo"
    val bar: String get() = "bar"
    var baz: String = "baz"
}

interface I {
    fun qux(): String = "qux"
}

open class WithParam(val s: String)

// Top-level property initializer
val topLevelProperty: String = object : Base(), I {}.foo()

// Top-level property accessors
var topLevelPropertyWithAccessors: String
    get() = object : Base() {}.bar
    set(value) { object : Base() {}.baz = value }

// Delegated property (lambda passed to delegate)
val lazyProperty: String by lazy { object : Base() {}.foo() }

// Function parameter default value + function body
fun topLevelFunction(p: String = object : Base() {}.foo()): String {
    val o = object : Base(), I {}
    o.baz = o.foo() + o.bar + o.qux() + p
    return o.baz
}

// Inline function body (the original KT-72296 case)
inline fun inlineFunction(): String = object : Base(), I {}.qux()

fun localDeclarations(): String {
    // Local function body
    fun local() = object : Base() {}.foo()
    // Lambda body
    val lambda = { object : Base() {}.bar }
    // Callable reference to a local fake override
    val ref = object : Base() {}::foo
    return local() + lambda() + ref()
}

class Container(
    // Constructor parameter default value
    p: String = object : Base() {}.foo()
) : WithParam(object : Base() {}.bar) { // super constructor call argument
    // Member property initializer
    val memberProperty: String = object : Base() {}.foo()

    // Member property getter
    val memberPropertyWithGetter: String
        get() = object : Base() {}.bar

    // Anonymous initializer body
    init {
        object : Base() {}.baz = p
    }

    // Secondary constructor: delegating call argument + body
    constructor(x: Int) : this(object : Base() {}.foo() + x) {
        object : Base(), I {}.qux()
    }

    // Member function body
    fun memberFunction(): String = object : Base(), I {}.qux()
}

// Enum entry constructor argument
enum class E(val s: String) {
    A(object : Base() {}.foo())
}

// Object declaration bodies
object Obj {
    val objProperty: String = object : Base() {}.bar
    init { object : Base() {}.baz = "obj" }
}

// Anonymous class calling its own fake overrides from its own bodies.
// Private, so the property type is the anonymous class itself (the KT-91111 shape).
private val anonymous = object : Base(), I {
    // Property initializer of anonymous class
    val a: String = foo()

    // Initializer of anonymous class
    init { baz = bar }

    // Member function of anonymous class
    fun b(): String = qux() + a

    // Getter of anonymous class property
    val c: String get() = foo() + bar
}

// Calls to the anonymous class's fake overrides from outside it
val anonymousValue: String
    get() = anonymous.foo() + anonymous.bar + anonymous.qux() + anonymous.b() + anonymous.c

// Second base with distinct member names, so that inside the inner anonymous class
// unqualified calls can resolve both to the inner's own fake overrides and to the outer's.
open class OtherBase {
    fun other(): String = "other"
}

// Anonymous class with local fake overrides nested in another anonymous class with local fake overrides
private val outer = object : Base(), I {
    // Inner anonymous class in a property initializer of the outer one
    private val inner = object : OtherBase() {
        // Own fake override (inner) + fake overrides of the outer anonymous class
        val x: String = other() + foo() + bar

        init { baz = other() + qux() }

        fun y(): String = other() + foo()
    }

    // Outer bodies calling fake overrides of the inner anonymous class
    val a: String = inner.other() + inner.x

    init { baz = inner.other() + inner.y() }

    fun b(): String {
        // Inner anonymous class declared in a function body of the outer one
        val local = object : OtherBase(), I {
            fun z(): String = other() + qux() + foo() // qux() resolves to the inner's own fake override
        }
        return local.other() + local.qux() + local.z()
    }

    val c: String get() = object : OtherBase() {}.other() + inner.other()
}

val outerValue: String
    get() = outer.foo() + outer.qux() + outer.a + outer.b() + outer.c

open class GenericBase<T>(val value: T) {
    fun get(): T = value
}

// Other shapes of local fake overrides, each one exercised in the body of a top-level function
fun otherShapes(): String {
    // Property reference to a local fake override
    val propertyRef = object : Base() {}::bar

    // Anonymous class with a concrete type argument for a generic base class
    val generic = object : GenericBase<String>("generic") {}.get()

    // Call of an Any member on an anonymous object
    val string = object : Base(), I {}.toString()

    // Named local class
    class Local : Base(), I
    val local = Local().foo() + Local().qux()

    // Anonymous object with only an interface supertype
    val onlyInterface = object : I {}.qux()

    return propertyRef.get() + generic + string + local + onlyInterface
}
