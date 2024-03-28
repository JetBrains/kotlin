// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyHidesFromObjC<!>()

@MyHidesFromObjC
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyHiddenFromObjC<!>()

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyRefinesInSwift<!>()

@MyRefinesInSwift
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyShouldRefineInSwift<!>()

<!INVALID_REFINES_IN_SWIFT_TARGETS{NATIVE}!>@MyRefinesInSwift<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class <!NO_ACTUAL_FOR_EXPECT, NO_ACTUAL_FOR_EXPECT{NATIVE}!>MyWrongShouldRefineInSwift<!>()

// FILE: plugin.kt
@MyHidesFromObjC
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class PluginMyHiddenFromObjC

@MyRefinesInSwift
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PluginMyShouldRefineInSwift

// FILE: main.kt
@MyHidesFromObjC
<!REDUNDANT_SWIFT_REFINEMENT{NATIVE}!>@MyRefinesInSwift<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationA

<!INVALID_OBJC_HIDES_TARGETS{NATIVE}!>@MyHidesFromObjC<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationB

<!INVALID_REFINES_IN_SWIFT_TARGETS{NATIVE}!>@MyRefinesInSwift<!>
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationC

@MyRefinesInSwift
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationD

typealias HFOC = MyHiddenFromObjC

@HFOC
<!REDUNDANT_SWIFT_REFINEMENT{NATIVE}!>@MyShouldRefineInSwift<!>
var refinedProperty: Int = 0

@PluginMyHiddenFromObjC
<!REDUNDANT_SWIFT_REFINEMENT{NATIVE}!>@PluginMyShouldRefineInSwift<!>
fun pluginRefinedFunction() { }

@MyHiddenFromObjC
@PluginMyHiddenFromObjC
fun multipleObjCRefinementsFunction() { }

@MyShouldRefineInSwift
@PluginMyShouldRefineInSwift
fun multipleSwiftRefinementsFunction() { }

@MyHiddenFromObjC
@PluginMyHiddenFromObjC
<!REDUNDANT_SWIFT_REFINEMENT{NATIVE}!>@MyShouldRefineInSwift<!>
<!REDUNDANT_SWIFT_REFINEMENT{NATIVE}!>@PluginMyShouldRefineInSwift<!>
fun multipleMixedRefinementsFunction() { }

interface InterfaceA {
    val barA: Int
    val barB: Int
    fun fooA()
    @MyHiddenFromObjC
    fun fooB()
}

interface InterfaceB {
    val barA: Int
    @MyShouldRefineInSwift
    val barB: Int
    @HFOC
    fun fooA()
    @MyHiddenFromObjC
    fun fooB()
}

open class ClassA: InterfaceA, InterfaceB {
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE{NATIVE}!>@MyHiddenFromObjC<!>
    override val barA: Int = 0
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE{NATIVE}!>@MyShouldRefineInSwift<!>
    override val barB: Int = 0
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE{NATIVE}!>override fun fooA() { }<!>
    override fun fooB() { }
    @MyHiddenFromObjC
    open fun fooC() { }
}

class ClassB: ClassA() {
    @MyHiddenFromObjC
    override fun fooB() { }
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE{NATIVE}!>@MyShouldRefineInSwift<!>
    override fun fooC() { }
}

open class Base {
    @MyHiddenFromObjC
    open fun foo() {}
}

interface I {
    fun foo()
}

<!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE{NATIVE}!>open class Derived : Base(), I<!>

open class Derived2 : Derived() {
    override fun foo() {}
}

@MyHiddenFromObjC
open class OpenHiddenClass

<!SUBTYPE_OF_HIDDEN_FROM_OBJC{NATIVE}!>class InheritsFromOpenHiddenClass : OpenHiddenClass()<!>

@MyHiddenFromObjC
interface HiddenInterface

interface NotHiddenInterface

<!SUBTYPE_OF_HIDDEN_FROM_OBJC{NATIVE}!>class ImplementsHiddenInterface : NotHiddenInterface, HiddenInterface<!>

<!SUBTYPE_OF_HIDDEN_FROM_OBJC{NATIVE}!>class InheritsFromOpenHiddenClass2 : NotHiddenInterface, OpenHiddenClass()<!>

@MyHiddenFromObjC
class OuterHidden {
    class Nested {
        open class Nested
    }
}

<!SUBTYPE_OF_HIDDEN_FROM_OBJC{NATIVE}!>class InheritsFromNested : OuterHidden.Nested.Nested()<!>

private class PrivateInheritsFromNested : OuterHidden.Nested.Nested()

internal class InternalInheritsFromNested : OuterHidden.Nested.Nested()

fun produceInstanceOfHidden(): OuterHidden.Nested.Nested {
    return object : OuterHidden.Nested.Nested() {}
}

@MyHiddenFromObjC
enum class MyHiddenEnum {
    A,
    B,
    C
}

@MyHiddenFromObjC
object MyHiddenObject

sealed class MySealedClass {
    @MyHiddenFromObjC
    class MyHiddenSealedVariant : MySealedClass()

    class MyPublicVariant : MySealedClass()
}

@MyHiddenFromObjC
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


// MODULE: platform()()(common)
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

// FILE: platform.kt

actual typealias MyHidesFromObjC = kotlin.native.HidesFromObjC
actual typealias MyHiddenFromObjC = kotlin.native.HiddenFromObjC
actual typealias MyRefinesInSwift = kotlin.native.RefinesInSwift
actual typealias MyShouldRefineInSwift = kotlin.native.ShouldRefineInSwift
