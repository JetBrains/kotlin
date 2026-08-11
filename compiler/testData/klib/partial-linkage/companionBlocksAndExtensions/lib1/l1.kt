class A {
    companion {
        val valChange = "propertyChange.v1"
        val removedVal = 42
        var varChange = valChange
        var removedVar = 42

        fun bodyChange() = "bodyChange.v1"
        fun removedFun() {}

        fun removedClass(): Any = RemovedClass(42)
        fun removedClassValue() = RemovedClass(42).value
        fun removedClassParameter(p: RemovedClass) {}
        fun <T : RemovedClass> removedClassTypeParameter() {}
    }

    companion {
        fun blockToObject() = "blockToObject"
        fun blockToCompanionExtension() = "blockToCompanionExtension"
        fun blockToRegularExtension() = "blockToRegularExtension"
    }

    companion object {
        fun objectToBlock() = "objectToBlock"
    }
}

companion fun A.companionExtensionToBlock() = "companionExtensionToBlock"
companion fun A.companionToRegularExtension() = "companionToRegularExtension"
fun A.regularToCompanionExtension() = "regularToCompanionExtension"
fun A.regularExtensionToBlock() = "regularExtensionToBlock"

class RemovedBlock {
    companion {
        fun sameFun() = "block"
    }

    companion object {
        fun sameFun() = "object"
    }
}

class NewBlock {
    companion object {
        fun sameFun() = "object"
    }
}

class RemovedClass(val value: Int)

class B {
    companion object {
        val removedCompanionVal = 42
        var removedCompanionVar = 42
        fun removedCompanionFun() = "removedCompanionFun"
    }
}

class PrivateClass
companion fun PrivateClass.privateClassFun() = "privateClassFun"

typealias TA = A
companion fun TA.aliasFun() = "aliasFun"
companion fun TA.aliasToClassFun() = "aliasToClassFun"
companion fun B.classToAliasFun() = "classToAliasFun"
