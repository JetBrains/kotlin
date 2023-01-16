// FIR_IDENTICAL
// FILE: kotlin.kt
package kotlin.native

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class HidesFromObjC

@HidesFromObjC
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class HiddenFromObjC

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class RefinesInSwift

@RefinesInSwift
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class ShouldRefineInSwift

// FILE: plugin.kt
package plugin

@HidesFromObjC
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PluginHiddenFromObjC

@RefinesInSwift
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PluginShouldRefineInSwift

// FILE: test.kt
import plugin.PluginHiddenFromObjC
import plugin.PluginShouldRefineInSwift

@HidesFromObjC
<!REDUNDANT_SWIFT_REFINEMENT!>@RefinesInSwift<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationA

<!INVALID_OBJC_REFINEMENT_TARGETS!>@HidesFromObjC<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationB

<!INVALID_OBJC_REFINEMENT_TARGETS!>@RefinesInSwift<!>
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationC

@RefinesInSwift
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationD

typealias HFOC = HiddenFromObjC

@HFOC
<!REDUNDANT_SWIFT_REFINEMENT!>@ShouldRefineInSwift<!>
var refinedProperty: Int = 0

@PluginHiddenFromObjC
<!REDUNDANT_SWIFT_REFINEMENT!>@PluginShouldRefineInSwift<!>
fun pluginRefinedFunction() { }

@HiddenFromObjC
@PluginHiddenFromObjC
fun multipleObjCRefinementsFunction() { }

@ShouldRefineInSwift
@PluginShouldRefineInSwift
fun multipleSwiftRefinementsFunction() { }

@HiddenFromObjC
@PluginHiddenFromObjC
<!REDUNDANT_SWIFT_REFINEMENT!>@ShouldRefineInSwift<!>
<!REDUNDANT_SWIFT_REFINEMENT!>@PluginShouldRefineInSwift<!>
fun multipleMixedRefinementsFunction() { }

interface InterfaceA {
    val barA: Int
    val barB: Int
    fun fooA()
    @HiddenFromObjC
    fun fooB()
}

interface InterfaceB {
    val barA: Int
    @ShouldRefineInSwift
    val barB: Int
    @HFOC
    fun fooA()
    @HiddenFromObjC
    fun fooB()
}

open class ClassA: InterfaceA, InterfaceB {
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE!>@HiddenFromObjC<!>
    override val barA: Int = 0
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE!>@ShouldRefineInSwift<!>
    override val barB: Int = 0
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE!>override fun fooA() { }<!>
    override fun fooB() { }
    @HiddenFromObjC
    open fun fooC() { }
}

class ClassB: ClassA() {
    @HiddenFromObjC
    override fun fooB() { }
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE!>@ShouldRefineInSwift<!>
    override fun fooC() { }
}

open class Base {
    @HiddenFromObjC
    open fun foo() {}
}

interface I {
    fun foo()
}

<!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE!>open class Derived : Base(), I<!>

open class Derived2 : Derived() {
    override fun foo() {}
}
