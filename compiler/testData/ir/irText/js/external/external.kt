// TARGET_BACKEND: JS_IR
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

external fun topLevelFun(a: Int): String = definedExternally
external var topLevelVar1: String = definedExternally
external var topLevelVar2: String
    get() = definedExternally
    set(value) = definedExternally

external interface ExternalInterface {
    fun getNestedInterface(): NestedInterface

    fun externalInterfaceFun(a: Int): String
    var externalInterfaceVar: String

    interface NestedInterface {
        fun nestedInterfaceFun(a: Int): String
        var nestedInterfaceVar: String
    }

    companion object {
        fun getExternalInterface(): ExternalInterface = definedExternally

        fun companionObjectFun(a: Int): String = definedExternally
        var companionObjectVar1: String = definedExternally
        var companionObjectVar2: String
            get() = definedExternally
            set(value) = definedExternally
    }
}

external class ExternalClass {
    fun getNestedInterface(): NestedInterface = definedExternally

    fun externalClassFun(a: Int): String = definedExternally
    var externalClassVar1: String = definedExternally
    var externalClassVar2: String
        get() = definedExternally
        set(value) = definedExternally

    interface NestedInterface {
        fun nestedInterfaceFun(a: Int): String
        var nestedInterfaceVar: String
    }

    class NestedClass {
        fun nestedClassFun(a: Int): String = definedExternally
        var nestedClassVar1: String = definedExternally
        var nestedClassVar2: String
            get() = definedExternally
            set(value) = definedExternally
    }

    object NestedObject {
        fun nestedObjectFun(a: Int): String = definedExternally
        var nestedObjectVar1: String = definedExternally
        var nestedObjectVar2: String
            get() = definedExternally
            set(value) = definedExternally
    }
}

// MODULE: app(lib)
// FILE: main.kt

fun main() {
    println(topLevelFun(42))
    topLevelVar1 = "a"
    println(topLevelVar1)
    topLevelVar2 = "a"
    println(topLevelVar2)

    with(ExternalInterface.getExternalInterface()) {
        println(externalInterfaceFun(42))
        externalInterfaceVar = "a"
        println(externalInterfaceVar)

        with(getNestedInterface()) {
            println(nestedInterfaceFun(42))
            nestedInterfaceVar = "a"
            println(nestedInterfaceVar)
        }
    }

    with(ExternalInterface.Companion) {
        println(companionObjectFun(42))
        companionObjectVar1 = "a"
        println(companionObjectVar1)
        companionObjectVar2 = "a"
        println(companionObjectVar2)
    }

    with(ExternalClass()) {
        println(externalClassFun(42))
        externalClassVar1 = "a"
        println(externalClassVar1)
        externalClassVar2 = "a"
        println(externalClassVar2)

        with(getNestedInterface()) {
            println(nestedInterfaceFun(42))
            nestedInterfaceVar = "a"
            println(nestedInterfaceVar)
        }
    }

    with(ExternalClass.NestedClass()) {
        println(nestedClassFun(42))
        nestedClassVar1 = "a"
        println(nestedClassVar1)
        nestedClassVar2 = "a"
        println(nestedClassVar2)
    }

    with(ExternalClass.NestedObject) {
        println(nestedObjectFun(42))
        nestedObjectVar1 = "a"
        println(nestedObjectVar1)
        nestedObjectVar2 = "a"
        println(nestedObjectVar2)
    }
}
