// RUN_PIPELINE_TILL: CODEGEN
// On Wasm, @JsName on a non-external, non-exported declaration has no effect: the declaration is compiled to Wasm,
// and no JS name is ever generated for it. Such usages are not reported yet, but they should be.
@file:OptIn(ExperimentalJsExport::class)

package foo

// Top level
@JsName("topLevelFun_") fun topLevelFun() = 1

@JsName("topLevelVal_") val topLevelVal = 1

var topLevelVar: Int
    @JsName("getTopLevelVar_") get() = 1
    @JsName("setTopLevelVar_") set(value) {}

@JsName("TopLevelClass_") class TopLevelClass

// Class members
open class A @JsName("A_") constructor(x: Int) {
    @JsName("fromString") constructor(s: String) : this(s.length)

    @JsName("foo_") fun foo() = 1

    @JsName("bar_") open val bar = 1

    var baz: Int
        @JsName("getBaz_") get() = 1
        @JsName("setBaz_") set(value) {}

    @JsName("Nested_") class Nested {
        @JsName("nestedFoo_") fun nestedFoo() = 1

        @JsName("DeeplyNested_") class DeeplyNested {
            @JsName("deeplyNestedFoo_") fun deeplyNestedFoo() = 1
        }
    }

    @JsName("Inner_") inner class Inner {
        @JsName("innerFoo_") fun innerFoo() = 1
    }

    @JsName("NestedObject_") object NestedObject {
        @JsName("nestedObjectFoo_") fun nestedObjectFoo() = 1
    }

    companion object {
        @JsName("companionFoo_") fun companionFoo() = 1

        @JsName("companionVal_") val companionVal = 1
    }
}

class B : A(1) {
    @JsName("bar__") override val bar = 2
}

// Objects and interfaces
@JsName("TopLevelObject_") object TopLevelObject {
    @JsName("objectFoo_") fun objectFoo() = 1

    @JsName("ObjectNested_") class ObjectNested {
        @JsName("objectNestedFoo_") fun objectNestedFoo() = 1
    }
}

@JsName("I_") interface I {
    @JsName("abstractFoo_") fun abstractFoo(): Int

    @JsName("defaultFoo_") fun defaultFoo() = 1

    @JsName("abstractVal_") val abstractVal: Int
}

// Enums
@JsName("E_") enum class E {
    ENTRY {
        @JsName("entryFoo_") fun entryFoo() = 1
    };

    @JsName("enumFoo_") fun enumFoo() = 1
}

// Extensions
@JsName("extFun_") fun A.extFun() = 1

val A.extVal: Int
    @JsName("getExtVal_") get() = 1

// Local declarations
fun locals(): Int {
    @JsName("localFun_") fun localFun() = 1

    @JsName("LocalClass_") class LocalClass {
        @JsName("localClassFoo_") fun localClassFoo() = 1
    }

    val anonymous = object {
        @JsName("anonymousFoo_") fun anonymousFoo() = 1
    }

    return localFun() + LocalClass().localClassFoo() + anonymous.anonymousFoo()
}

// Valid: on exported declarations @JsName sets the exported name, so these must stay unreported
@JsExport @JsName("exportedFun_") fun exportedFun() = 1
