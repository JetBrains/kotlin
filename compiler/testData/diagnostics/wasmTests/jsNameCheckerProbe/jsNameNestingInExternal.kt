// RUN_PIPELINE_TILL: CODEGEN
// @JsName on declarations that are external through the nesting: members of external classes, objects and interfaces,
// and their nested classifiers.
package foo

@JsName("A_") open external class A {
    @JsName("foo_") fun foo(): Int

    @JsName("Nested_") open class Nested {
        @JsName("nestedFoo_") fun nestedFoo(): Int

        @JsName("DeeplyNested_") class DeeplyNested {
            @JsName("deeplyNestedFoo_") fun deeplyNestedFoo(): Int

            var deeplyNestedVar: Int
                @JsName("getDeeplyNestedVar_") get
                @JsName("setDeeplyNestedVar_") set
        }
    }

    @JsName("NestedObject_") object NestedObject {
        @JsName("nestedObjectFoo_") fun nestedObjectFoo(): Int

        @JsName("nestedObjectVal_") val nestedObjectVal: Int
    }

    @JsName("NestedInterface_") interface NestedInterface {
        @JsName("nestedInterfaceFoo_") fun nestedInterfaceFoo(): Int
    }

    companion object {
        @JsName("companionFoo_") fun companionFoo(): Int

        @JsName("CompanionNested_") class CompanionNested {
            @JsName("companionNestedFoo_") fun companionNestedFoo(): Int
        }
    }
}

@JsName("B_") external class B : A {
    @JsName("Nested_") class Nested : A.Nested {
        @JsName("nestedFoo__") fun nestedFoo2(): Int
    }
}

@JsName("O_") external object O {
    @JsName("objectFoo_") fun objectFoo(): Int

    @JsName("ObjectNested_") class ObjectNested {
        @JsName("objectNestedFoo_") fun objectNestedFoo(): Int
    }

    @JsName("ObjectNestedObject_") object ObjectNestedObject {
        @JsName("ObjectNestedObjectNested_") class ObjectNestedObjectNested {
            @JsName("objectNestedObjectNestedFoo_") fun objectNestedObjectNestedFoo(): Int
        }
    }
}

@JsName("I_") external interface I {
    @JsName("interfaceFoo_") fun interfaceFoo(): Int

    @JsName("interfaceVal_") val interfaceVal: Int

    var interfaceVar: Int
        @JsName("getInterfaceVar_") get
        @JsName("setInterfaceVar_") set
}
