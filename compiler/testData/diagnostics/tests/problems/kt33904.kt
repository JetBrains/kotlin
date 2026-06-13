// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-33904

// KT-33904: Compiler freezes / infinite loop in constraint incorporation with recursive generic type parameters

class Inv<T>

class ServiceInterfaces<
    STATE,
    SERVICE : ModuleBasedServiceWithProjectDefault<STATE, SERVICE, APP_SERVICE, MODULE_SERVICE>,
    APP_SERVICE : SERVICE,
    MODULE_SERVICE : SERVICE>
{
    val moduleServiceClass: Inv<MODULE_SERVICE>
        get() = TODO()

    val appServiceClass: Inv<APP_SERVICE>
        get() = TODO()
}

class ModuleBasedServiceWithProjectDefault<
    STATE,
    SERVICE : ModuleBasedServiceWithProjectDefault<STATE, SERVICE, APP_SERVICE, MODULE_SERVICE>,
    APP_SERVICE : SERVICE,
    MODULE_SERVICE : SERVICE>
{
    companion object {
        fun <STATE,
            SERVICE : ModuleBasedServiceWithProjectDefault<STATE, SERVICE, APP_SERVICE, MODULE_SERVICE>,
            APP_SERVICE : SERVICE,
            MODULE_SERVICE : SERVICE> getService(
            module: SomeModule?,
            interfaces: ServiceInterfaces<STATE, SERVICE, APP_SERVICE, MODULE_SERVICE>
        ): SERVICE {
            val a: SERVICE = module?.let { getServiceM(interfaces.moduleServiceClass) } ?: getServiceM(interfaces.appServiceClass)
            val b: SERVICE = module?.let { getServiceM(interfaces.moduleServiceClass) } ?: getServiceM(interfaces.appServiceClass)
            return module?.let { getServiceM(interfaces.moduleServiceClass) } ?: getServiceM(interfaces.appServiceClass)
        }
    }
}

class SomeModule

fun <T> getServiceM(otherInv: Inv<T>): T = TODO()

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, elvisExpression, functionDeclaration, getter, lambdaLiteral,
localProperty, nullableType, objectDeclaration, propertyDeclaration, safeCall, typeConstraint, typeParameter */
