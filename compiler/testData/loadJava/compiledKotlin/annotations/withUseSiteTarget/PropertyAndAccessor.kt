// PLATFORM_DEPENDANT_METADATA
package test

annotation class A(val value: String)
annotation class B(val value: Array<String>)

interface I {
    @A("property")
    @get:B(["getter"])
    var propertyAndGetter: Int

    @A("property")
    @set:B(["setter"])
    var propertyAndSetter: Int

    @get:A("getter")
    @set:B(["setter"])
    var getterAndSetter: Int
}
