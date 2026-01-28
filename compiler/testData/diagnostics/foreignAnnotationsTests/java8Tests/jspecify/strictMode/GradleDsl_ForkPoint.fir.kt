// ISSUE: KT-83824
// JSPECIFY_STATE: strict
// LANGUAGE: +CheckPackageInfoNullnessAnnotations

// FILE: api/action/Action.java

package api.action;

public interface Action<A> {
    void execute(A t);
}

// FILE: api/package-info.java

@NullMarked
package api;

import org.jspecify.annotations.*;

// FILE: api/Owner.java

package api;

import artifacts.*;

public interface Owner {
    ConfigurationContainer getConfigurations();
}

// FILE: artifacts/package-info.java

@NullMarked
package artifacts;

import org.jspecify.annotations.*;

// FILE: artifacts/ConfigurationContainer.java

package artifacts;

import api.*;

public interface ConfigurationContainer extends NamedDomainObjectContainer<Configuration> {

}

// FILE: artifacts/Configuration.java

package artifacts;

import attributes.*;

public interface Configuration extends HasConfigurableAttributes<Configuration> {}

// FILE: attributes/HasConfigurableAttributes.java

package attributes;

public interface HasConfigurableAttributes<SELF> {}

// FILE: api/NamedDomainObjectContainer.java

package api;

public interface NamedDomainObjectContainer<NC> {
    NamedDomainObjectProvider<NC> register(String name);
}

// FILE: api/NamedDomainObjectProvider.java

package api;

public interface NamedDomainObjectProvider<NP> {}

// FILE: file/ConfigurableFileCollection.java

package file;

public interface ConfigurableFileCollection {}

// FILE: dsl.kt

package dsl

import kotlin.reflect.*
import api.*
import api.action.*
import artifacts.*
import file.*

operator fun ConfigurationContainer.invoke(configuration: Action<ConfigurationContainerScope>): ConfigurationContainer = apply {}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <IT : Any, IC : NamedDomainObjectContainer<IT>> IC.invoke(configuration: Action<NamedDomainObjectContainerScope<IT>>): IC = this

inline val <RT : Any, RC : NamedDomainObjectContainer<RT>> RC.registering: RegisteringDomainObjectDelegateProvider<out RC>
    get() = RegisteringDomainObjectDelegateProvider.of(this)

class RegisteringDomainObjectDelegateProvider<RP> private constructor(val delegateProvider: RP) {
    companion object {
        fun <RPO> of(delegateProvider: RPO) = RegisteringDomainObjectDelegateProvider(delegateProvider)
    }
}

class ExistingDomainObjectDelegate<ED> private constructor(val delegate: ED) {
    companion object {
        fun <EDO> of(delegate: EDO) = ExistingDomainObjectDelegate(delegate)
    }
}

class ConfigurationContainerScope private constructor(
    override val delegate: ConfigurationContainer
) : NamedDomainObjectContainerScope<Configuration>(delegate), ConfigurationContainer {
    companion object {
        fun of(container: ConfigurationContainer) = ConfigurationContainerScope(container)
    }
}

open class NamedDomainObjectContainerScope<NS : Any> internal constructor(
    override val delegate: NamedDomainObjectContainer<NS>
) : NamedDomainObjectContainerDelegate<NS>()

abstract class NamedDomainObjectContainerDelegate<ND : Any> : NamedDomainObjectContainer<ND> {
    internal abstract val delegate: NamedDomainObjectContainer<ND>

    override fun register(name: String): NamedDomainObjectProvider<ND> = delegate.register(name)
}

operator fun <PDT : Any, PDC : NamedDomainObjectContainer<PDT>> RegisteringDomainObjectDelegateProvider<PDC>.provideDelegate(
    receiver: Any?,
    property: KProperty<*>
) = ExistingDomainObjectDelegate.of(delegateProvider.register(property.name))

operator fun <GV> ExistingDomainObjectDelegate<out GV>.getValue(receiver: Any?, property: KProperty<*>): GV = delegate

// FILE: test.kt

package dsl

import api.Owner
import api.NamedDomainObjectProvider
import artifacts.Configuration

fun Owner.foo() {
    configurations.invoke /* (Action<ConfigurationContainerScope>) */ { /* it: ConfigurationContainerScope! */
        // class ConfigurationContainerScope : NamedDomainObjectContainerScope<Configuration>
        // class NamedDomainObjectContainerScope<T : Any> : NamedDomainObjectContainerDelegate<T>
        // class NamedDomainObjectContainerDelegate<T : Any> : NamedDomainObjectContainer<T>
        val valley: NamedDomainObjectProvider<Configuration> <!DELEGATE_SPECIAL_FUNCTION_MISSING!>by<!> <!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>it.registering<!>
        /* it.registering<Configuration, ConfigurationContainerScope!> : RegisteringDomainObjectDelegateProvider<captured(out ConfigurationContainerScope!)>
              .provideDelegate<Configuration, captured(out ConfigurationContainerScope!)> : ExistingDomainObjectDelegate<ConfigurationContainerScope!>
         */
        it.registering.provideDelegate(null, ::<!UNSUPPORTED("References to variables aren't supported yet")!>valley<!>).hashCode()
        with(it.registering) {
            provideDelegate(null, ::<!UNSUPPORTED!>valley<!>).hashCode()
        }
    }
}

// provideDelegate
// actualType = RegisteringDomainObjectDelegatProvide<CapturedType(out RC)>
// expectedType = RegisteringDomainObjectDelegateProvider<PDC>

// RT <: Any
// RC <: NamedDomainObjectContainer<RT>
// ConfigurationContainerScope! <: RC
// PDT : Any
// PDC <: NamedDomainObjectContainer<PDT>
// RegisteringDomainObjectDelegateProvider<CapturedType(out RC)> <: RegisteringDomainObjectDelegateProvider<PDC>

// Incorporate: ConfigurationContainerScope! <: CapturedType(out TypeVariable(RC)) ==> FAIL

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, companionObject, flexibleType, funWithExtensionReceiver,
functionDeclaration, getter, javaProperty, javaType, lambdaLiteral, localProperty, nullableType, objectDeclaration,
operator, outProjection, override, primaryConstructor, propertyDeclaration, propertyDelegate,
propertyWithExtensionReceiver, samConversion, starProjection, thisExpression, typeConstraint, typeParameter */
