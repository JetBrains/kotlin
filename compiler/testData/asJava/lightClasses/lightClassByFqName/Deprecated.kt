// foo.DeprecatedClass
// WITH_STDLIB
package foo

@Deprecated("deprecated class", ReplaceWith("new class"), level = DeprecationLevel.ERROR)
class DeprecatedClass {
    @Deprecated("error function", ReplaceWith("function"), level = DeprecationLevel.ERROR)
    fun deprecatedErrorFunction() {

    }

    @Deprecated("error variable", ReplaceWith("property"), level = DeprecationLevel.ERROR)
    var deprecatedErrorVariable: Int = 1

    @get:Deprecated("error getter", ReplaceWith("new getter"), level = DeprecationLevel.ERROR)
    var deprecatedErrorAccessors: String = "2"
        @Deprecated("error setter", ReplaceWith("new setter"), level = DeprecationLevel.ERROR)
        set

    @Deprecated("deprecated function")
    fun deprecatedFunction(i: Int) {}

    @Deprecated("deprecated variable")
    var deprecatedVariable: Boolean = false

    @get:Deprecated("deprecated getter")
    var deprecatedAccessors: String = "2"
        @Deprecated("deprecated setter")
        set
}
