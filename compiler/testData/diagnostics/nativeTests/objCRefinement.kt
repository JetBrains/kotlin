// FIR_IDENTICAL
// FILE: kotlin.kt
package kotlin.native

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class HidesFromObjC

@HidesFromObjC
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class HiddenFromObjC

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class RefinesInSwift

@RefinesInSwift
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class ShouldRefineInSwift

<!INVALID_REFINES_IN_SWIFT_TARGETS!>@RefinesInSwift<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class WrongShouldRefineInSwift

// FILE: plugin.kt
package plugin

@HidesFromObjC
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
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

<!INVALID_OBJC_HIDES_TARGETS!>@HidesFromObjC<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationB

<!INVALID_REFINES_IN_SWIFT_TARGETS!>@RefinesInSwift<!>
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

@HiddenFromObjC
open class OpenHiddenClass

<!SUBTYPE_OF_HIDDEN_FROM_OBJC!>class InheritsFromOpenHiddenClass : OpenHiddenClass()<!>

@HiddenFromObjC
interface HiddenInterface

interface NotHiddenInterface

<!SUBTYPE_OF_HIDDEN_FROM_OBJC!>class ImplementsHiddenInterface : NotHiddenInterface, HiddenInterface<!>

<!SUBTYPE_OF_HIDDEN_FROM_OBJC!>class InheritsFromOpenHiddenClass2 : NotHiddenInterface, OpenHiddenClass()<!>

@HiddenFromObjC
class OuterHidden {
    class Nested {
        open class Nested
    }
}

<!SUBTYPE_OF_HIDDEN_FROM_OBJC!>class InheritsFromNested : OuterHidden.Nested.Nested()<!>

private class PrivateInheritsFromNested : OuterHidden.Nested.Nested()

internal class InternalInheritsFromNested : OuterHidden.Nested.Nested()

fun produceInstanceOfHidden(): OuterHidden.Nested.Nested {
    return object : OuterHidden.Nested.Nested() {}
}

@HiddenFromObjC
enum class MyHiddenEnum {
    A,
    B,
    C
}

@HiddenFromObjC
object MyHiddenObject

sealed class MySealedClass {
    @HiddenFromObjC
    class MyHiddenSealedVariant : MySealedClass()

    class MyPublicVariant : MySealedClass()
}

@HiddenFromObjC
enum class MyHiddenNonTrivialEnum {
    A,
    B,
    C {
        override fun sayCheese(): String {
            return "Boo :("
        }
    };

    open fun sayCheese(): String {
        return "Cheese!"
    }
}
