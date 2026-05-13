class Container {
    fun funExistingAnyOverload(arg: Any?): String = "Any?"
    fun funExistingAnyOverload(arg: RemovedClass): String = "RemovedClass"
    fun callfunExistingAnyOverloadWithRemovedClass(): String = funExistingAnyOverload(RemovedClass())
    
    fun funTwoOverloads(arg: RemovedClass): String = "RemovedClass"
    fun funTwoOverloads(arg: RemovedEnum): String = "RemovedEnum"
    fun callFunTwoOverloadsWithRemovedClass() = funTwoOverloads(RemovedClass())
    fun callFunTwoOverloadsWithRemovedEnum() = funTwoOverloads(RemovedEnum.A)

    fun<T : Any?> funExistingAnyOverloadTP(arg: T): String = "T : Any?"
    fun<T : RemovedClass> funExistingAnyOverloadTP(arg: T): String = "T : RemovedClass"
    fun callfunExistingAnyOverloadWithRemovedClassTP(): String = funExistingAnyOverloadTP<RemovedClass>(RemovedClass())

    fun<T: RemovedClass> funTwoOverloadsTP(arg: T): String = "T : RemovedClass"
    fun<T: RemovedEnum> funTwoOverloadsTP(arg: T): String = "T : RemovedEnum"
    fun callFunTwoOverloadsWithRemovedClassTP() = funTwoOverloadsTP<RemovedClass>(RemovedClass())
    fun callFunTwoOverloadsWithRemovedEnumTP() = funTwoOverloadsTP<RemovedEnum>(RemovedEnum.A)

}
