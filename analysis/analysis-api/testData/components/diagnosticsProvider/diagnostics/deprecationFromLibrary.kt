// IGNORE_FE10

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package one

@Deprecated("Deprecated class")
class MyDeprecatedClass {
    @property:Deprecated("Deprecated property")
    @get:Deprecated("Deprecated getter")
    @set:Deprecated("Deprecated setter")
    var deprecatedProperty: Int = 0
        get() = field
        set(value) {
            field = value
        }

    @Deprecated("Deprecated function")
    fun deprecatedFunction() {

    }

    @get:Deprecated("Deprecated getter")
    @set:Deprecated("Deprecated setter")
    var deprecatedAccessors: Int = 1
        get() = field
        set(value) {
            field = value
        }
}

typealias MyTypeAliasWithDeprecatedClass = MyDeprecatedClass

@Deprecated("Deprecated typealias")
typealias MyDeprecatedTypeAlias = Int

@property:Deprecated("Deprecated property")
@get:Deprecated("Deprecated getter")
@set:Deprecated("Deprecated setter")
var deprecatedProperty: Int = 2
    get() = field
    set(value) {
        field = value
    }

@get:Deprecated("Deprecated getter")
@set:Deprecated("Deprecated setter")
var deprecatedAccessors: Int = 3
    get() = field
    set(value) {
        field = value
    }

@Deprecated("Deprecated function")
fun deprecatedFunction() {

}

class MyClassWithDeprecatedConstructor @Deprecated("Deprecated constructor") constructor()

// MODULE: main(lib)
// FILE: usage.kt
package one

fun usage(
    deprecatedClass: MyDeprecatedClass,
    deprecatedTypealias: MyDeprecatedTypeAlias,
    typealiasWithDeprecatedClass: MyTypeAliasWithDeprecatedClass,
) {
    deprecatedClass.deprecatedProperty.toString()
    deprecatedClass.deprecatedProperty = 1

    deprecatedClass.deprecatedAccessors.toString()
    deprecatedClass.deprecatedAccessors = 2

    deprecatedClass.deprecatedFunction()

    deprecatedProperty.toString()
    deprecatedProperty = 3

    deprecatedAccessors.toString()
    deprecatedAccessors = 4

    deprecatedFunction()

    MyClassWithDeprecatedConstructor()
}