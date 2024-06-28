// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
@kotlin.experimental.ExperimentalObjCRefinement
expect annotation class MyHidesFromObjC()

@MyHidesFromObjC
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@kotlin.experimental.ExperimentalObjCRefinement
expect annotation class MyHiddenFromObjC()

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
@kotlin.experimental.ExperimentalObjCRefinement
expect annotation class MyRefinesInSwift()

@MyRefinesInSwift
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@kotlin.experimental.ExperimentalObjCRefinement
expect annotation class MyShouldRefineInSwift()

<!INVALID_REFINES_IN_SWIFT_TARGETS!>@MyRefinesInSwift<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class MyWrongShouldRefineInSwift()

// FILE: plugin.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

@MyHidesFromObjC
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class PluginMyHiddenFromObjC

@MyRefinesInSwift
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PluginMyShouldRefineInSwift

// FILE: main.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

@MyHidesFromObjC
<!REDUNDANT_SWIFT_REFINEMENT!>@MyRefinesInSwift<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationA

<!INVALID_OBJC_HIDES_TARGETS!>@MyHidesFromObjC<!>
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationB

<!INVALID_REFINES_IN_SWIFT_TARGETS!>@MyRefinesInSwift<!>
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationC

@MyRefinesInSwift
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class MyRefinedAnnotationD

typealias HFOC = MyHiddenFromObjC

@HFOC
<!REDUNDANT_SWIFT_REFINEMENT!>@MyShouldRefineInSwift<!>
var refinedProperty: Int = 0

@PluginMyHiddenFromObjC
<!REDUNDANT_SWIFT_REFINEMENT!>@PluginMyShouldRefineInSwift<!>
fun pluginRefinedFunction() { }

@MyHiddenFromObjC
@PluginMyHiddenFromObjC
fun multipleObjCRefinementsFunction() { }

@MyShouldRefineInSwift
@PluginMyShouldRefineInSwift
fun multipleSwiftRefinementsFunction() { }

@MyHiddenFromObjC
@PluginMyHiddenFromObjC
<!REDUNDANT_SWIFT_REFINEMENT!>@MyShouldRefineInSwift<!>
<!REDUNDANT_SWIFT_REFINEMENT!>@PluginMyShouldRefineInSwift<!>
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
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE!>@MyHiddenFromObjC<!>
    override val barA: Int = 0
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE!>@MyShouldRefineInSwift<!>
    override val barB: Int = 0
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE!>override fun fooA() { }<!>
    override fun fooB() { }
    @MyHiddenFromObjC
    open fun fooC() { }
}

class ClassB: ClassA() {
    @MyHiddenFromObjC
    override fun fooB() { }
    <!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE!>@MyShouldRefineInSwift<!>
    override fun fooC() { }
}

open class Base {
    @MyHiddenFromObjC
    open fun foo() {}
}

interface I {
    fun foo()
}

<!INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE!>open class Derived : Base(), I<!>

open class Derived2 : Derived() {
    override fun foo() {}
}

@MyHiddenFromObjC
open class OpenHiddenClass

class InheritsFromOpenHiddenClass : OpenHiddenClass()

@MyHiddenFromObjC
interface HiddenInterface

interface NotHiddenInterface

class ImplementsHiddenInterface : NotHiddenInterface, HiddenInterface

class InheritsFromOpenHiddenClass2 : NotHiddenInterface, OpenHiddenClass()

@MyHiddenFromObjC
class OuterHidden {
    class Nested {
        open class Nested
    }
}

class InheritsFromNested : OuterHidden.Nested.Nested()

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
// FILE: platform.kt
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

actual typealias MyHidesFromObjC = kotlin.native.HidesFromObjC
actual typealias MyHiddenFromObjC = kotlin.native.HiddenFromObjC
actual typealias MyRefinesInSwift = kotlin.native.RefinesInSwift
actual typealias MyShouldRefineInSwift = kotlin.native.ShouldRefineInSwift
