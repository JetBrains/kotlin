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
    }

    companion object {
        fun objectToBlock() = "objectToBlock"
    }
}

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
