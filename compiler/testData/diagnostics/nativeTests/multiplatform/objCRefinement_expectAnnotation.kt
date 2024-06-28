// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@kotlin.experimental.ExperimentalObjCRefinement
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyHidesFromObjC<!>()

@MyHidesFromObjC
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@kotlin.experimental.ExperimentalObjCRefinement
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyHiddenFromObjC<!>()

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@kotlin.experimental.ExperimentalObjCRefinement
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyRefinesInSwift<!>()

@MyRefinesInSwift
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@kotlin.experimental.ExperimentalObjCRefinement
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyShouldRefineInSwift<!>()

<!INVALID_REFINES_IN_SWIFT_TARGETS{NATIVE}!>@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyRefinesInSwift<!><!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class <!NO_ACTUAL_FOR_EXPECT, NO_ACTUAL_FOR_EXPECT{NATIVE}!>MyWrongShouldRefineInSwift<!>()

// FILE: plugin.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHidesFromObjC<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class PluginMyHiddenFromObjC

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyRefinesInSwift<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PluginMyShouldRefineInSwift

// FILE: main.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHidesFromObjC<!>
<!REDUNDANT_SWIFT_REFINEMENT{NATIVE}!>@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyRefinesInSwift<!><!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationA

<!INVALID_OBJC_HIDES_TARGETS{NATIVE}!>@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHidesFromObjC<!><!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationB

<!INVALID_REFINES_IN_SWIFT_TARGETS{NATIVE}!>@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyRefinesInSwift<!><!>
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationC

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyRefinesInSwift<!>
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

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
@PluginMyHiddenFromObjC
fun multipleObjCRefinementsFunction() { }

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyShouldRefineInSwift<!>
@PluginMyShouldRefineInSwift
fun multipleSwiftRefinementsFunction() { }

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
@PluginMyHiddenFromObjC
<!REDUNDANT_SWIFT_REFINEMENT{NATIVE}!>@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyShouldRefineInSwift<!><!>
<!REDUNDANT_SWIFT_REFINEMENT{NATIVE}!>@PluginMyShouldRefineInSwift<!>
fun multipleMixedRefinementsFunction() { }

interface InterfaceA {
    val barA: Int
    val barB: Int
    fun fooA()
    @<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
    fun fooB()
}

interface InterfaceB {
    val barA: Int
    @MyShouldRefineInSwift
    val barB: Int
    @<!OPT_IN_USAGE_ERROR, OPT_IN_USAGE_ERROR{NATIVE}!>HFOC<!>
    fun fooA()
    @<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
    fun fooB()
}

open class ClassA: InterfaceA, InterfaceB {
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE{NATIVE}!>@MyHiddenFromObjC<!>
    override val barA: Int = 0
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE{NATIVE}!>@MyShouldRefineInSwift<!>
    override val barB: Int = 0
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE{NATIVE}!>override fun fooA() { }<!>
    override fun fooB() { }
    @<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
    open fun fooC() { }
}

class ClassB: ClassA() {
    @<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
    override fun fooB() { }
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE{NATIVE}!>@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyShouldRefineInSwift<!><!>
    override fun fooC() { }
}

open class Base {
    @<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
    open fun foo() {}
}

interface I {
    fun foo()
}

<!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE{NATIVE}!>open class Derived : Base(), I<!>

open class Derived2 : Derived() {
    override fun foo() {}
}

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
open class OpenHiddenClass

<!SUBTYPE_OF_HIDDEN_FROM_OBJC{NATIVE}!>class InheritsFromOpenHiddenClass : OpenHiddenClass()<!>

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
interface HiddenInterface

interface NotHiddenInterface

<!SUBTYPE_OF_HIDDEN_FROM_OBJC{NATIVE}!>class ImplementsHiddenInterface : NotHiddenInterface, HiddenInterface<!>

<!SUBTYPE_OF_HIDDEN_FROM_OBJC{NATIVE}!>class InheritsFromOpenHiddenClass2 : NotHiddenInterface, OpenHiddenClass()<!>

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
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

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
enum class MyHiddenEnum {
    A,
    B,
    C
}

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
object MyHiddenObject

sealed class MySealedClass {
    @<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
    class MyHiddenSealedVariant : MySealedClass()

    class MyPublicVariant : MySealedClass()
}

@<!OPT_IN_USAGE_ERROR{NATIVE}!>MyHiddenFromObjC<!>
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
// FILE: platform.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

actual typealias MyHidesFromObjC = kotlin.native.HidesFromObjC
actual typealias MyHiddenFromObjC = kotlin.native.HiddenFromObjC
actual typealias MyRefinesInSwift = kotlin.native.RefinesInSwift
actual typealias MyShouldRefineInSwift = kotlin.native.ShouldRefineInSwift
