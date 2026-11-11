// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// ISSUE: KT-91441
import kotlin.reflect.KProperty

class StaticDelegate {
    operator fun getValue(thisRef: Nothing?, property: KProperty<*>): Int = 1
    operator fun setValue(thisRef: Nothing?, property: KProperty<*>, value: Int) {}
}

class StaticDelegateProvider {
    operator fun provideDelegate(thisRef: Nothing?, property: KProperty<*>): StaticDelegate = StaticDelegate()
}

class InstanceDelegate {
    operator fun getValue(thisRef: Owner, property: KProperty<*>): Int = 1
    operator fun setValue(thisRef: Owner, property: KProperty<*>, value: Int) {}
}

class InstanceDelegateProvider {
    operator fun provideDelegate(thisRef: Owner, property: KProperty<*>): StaticDelegate = StaticDelegate()
}

class Owner {
    companion {
        val staticVal: Int by StaticDelegate()
        var staticVar: Int by StaticDelegate()
        val staticProvided: Int by StaticDelegateProvider()

        val instanceVal: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> InstanceDelegate()
        var instanceVar: Int <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>by<!> InstanceDelegate()
        val instanceProvided: Int <!DELEGATE_SPECIAL_FUNCTION_MISSING!>by<!> InstanceDelegateProvider()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, operator,
propertyDeclaration, propertyDelegate, setter, starProjection */
