// ISSUE: KT-91111
// References to fake overrides of local classes outside inline functions, where AvoidLocalFakeOverridesLowering replaces
// their reflection targets with the members of the supertypes. Complements
// compiler/testData/codegen/boxInline/anonymousObject/fakeOverrideReferenceGenericBase.kt, which covers bound references in
// an inline function, with unbound references, a mutable property reference, references to interface members with default
// implementations, and the names of the referenced members.
// KLIBs written by compilers which apply the lowering only to inline functions keep such references, as legacy ones up to
// 2.2.x: with such a first-stage compiler, the second stage must link them and build the fake overrides they point to.

open class Base {
    fun foo(): String = "foo"
    val bar: String get() = "bar"
    var baz: String = "baz"
}

interface WithDefaults {
    fun qux(): String = "qux"
    val quux: String get() = "quux"
}

fun boundReferences(): String {
    val obj = object : Base(), WithDefaults {}
    val function = obj::foo
    val property = obj::bar
    val mutableProperty = obj::baz
    val interfaceFunction = obj::qux
    val interfaceProperty = obj::quux
    mutableProperty.set("BAZ")
    if (function.name != "foo" || property.name != "bar" || mutableProperty.name != "baz") return "Fail names"
    if (interfaceFunction.name != "qux" || interfaceProperty.name != "quux") return "Fail interface names"
    return function() + property.get() + mutableProperty.get() + interfaceFunction() + interfaceProperty.get()
}

fun unboundReferences(): String {
    class Local : Base(), WithDefaults

    val function = Local::foo
    val property = Local::bar
    val interfaceFunction = Local::qux
    return function(Local()) + property.get(Local()) + interfaceFunction(Local())
}

fun box(): String {
    val bound = boundReferences()
    if (bound != "foobarBAZquxquux") return "Fail bound: $bound"
    val unbound = unboundReferences()
    if (unbound != "foobarqux") return "Fail unbound: $unbound"
    return "OK"
}
