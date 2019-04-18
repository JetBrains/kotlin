/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IMPLEMENTING_FUNCTION_INTERFACE")
package kotlin.reflect

/**
 * Represents a property, such as a named `val` or `var` declaration.
 * Instances of this class are obtainable by the `::` operator.
 * 
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/reflection.html)
 * for more information.
 *
 * @param R the type of the property.
 */
public actual interface KProperty<out R> : KCallable<R> {
    /**
     * `true` if this property is `lateinit`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/properties.html#late-initialized-properties)
     * for more information.
     */
    @SinceKotlin("1.1")
    public val isLateinit: Boolean

    /**
     * `true` if this property is `const`.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/properties.html#compile-time-constants)
     * for more information.
     */
    @SinceKotlin("1.1")
    public val isConst: Boolean

    /** The getter of this property, used to obtain the value of the property. */
    public val getter: Getter<R>

    /**
     * Represents a property accessor, which is a `get` or `set` method declared alongside the property.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/properties.html#getters-and-setters)
     * for more information.
     *
     * @param R the type of the property, which it is an accessor of.
     */
    public interface Accessor<out R> {
        /** The property which this accessor is originated from. */
        public val property: KProperty<R>
    }

    /**
     * Getter of the property is a `get` method declared alongside the property.
     */
    public interface Getter<out R> : Accessor<R>, KFunction<R>
}

/**
 * Represents a property declared as a `var`.
 */
public actual interface KMutableProperty<R> : KProperty<R> {
    /** The setter of this mutable property, used to change the value of the property. */
    public val setter: Setter<R>

    /**
     * Setter of the property is a `set` method declared alongside the property.
     */
    public interface Setter<R> : KProperty.Accessor<R>, KFunction<Unit>
}


/**
 * Represents a property without any kind of receiver.
 * Such property is either originally declared in a receiverless context such as a package,
 * or has the receiver bound to it.
 */
public actual interface KProperty0<out R> : KProperty<R>, () -> R {
    /**
     * Returns the current value of the property.
     */
    public actual fun get(): R

    /**
     * Returns the value of the delegate if this is a delegated property, or `null` if this property is not delegated.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/delegated-properties.html)
     * for more information.
     */
    @SinceKotlin("1.1")
    public fun getDelegate(): Any?

    override val getter: Getter<R>

    /**
     * Getter of the property is a `get` method declared alongside the property.
     *
     * Can be used as a function that takes 0 arguments and returns the value of the property type [R].
     */
    public interface Getter<out R> : KProperty.Getter<R>, () -> R
}

/**
 * Represents a `var`-property without any kind of receiver.
 */
public actual interface KMutableProperty0<R> : KProperty0<R>, KMutableProperty<R> {
    /**
     * Modifies the value of the property.
     *
     * @param value the new value to be assigned to this property.
     */
    public actual fun set(value: R)

    override val setter: Setter<R>

    /**
     * Setter of the property is a `set` method declared alongside the property.
     *
     * Can be used as a function that takes new property value as an argument and returns [Unit].
     */
    public interface Setter<R> : KMutableProperty.Setter<R>, (R) -> Unit
}


/**
 * Represents a property, operations on which take one receiver as a parameter.
 *
 * @param T the type of the receiver which should be used to obtain the value of the property.
 * @param R the type of the property.
 */
public actual interface KProperty1<T, out R> : KProperty<R>, (T) -> R {
    /**
     * Returns the current value of the property.
     *
     * @param receiver the receiver which is used to obtain the value of the property.
     *                 For example, it should be a class instance if this is a member property of that class,
     *                 or an extension receiver if this is a top level extension property.
     */
    public actual fun get(receiver: T): R

    /**
     * Returns the value of the delegate if this is a delegated property, or `null` if this property is not delegated.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/delegated-properties.html)
     * for more information.
     *
     * Note that for a top level **extension** property, the delegate is the same for all extension receivers,
     * so the actual [receiver] instance passed in is not going to make any difference, it must only be a value of [T].
     *
     * @param receiver the receiver which is used to obtain the value of the property delegate.
     *                 For example, it should be a class instance if this is a member property of that class,
     *                 or an extension receiver if this is a top level extension property.
     *
     * @see [kotlin.reflect.full.getExtensionDelegate] // [KProperty1.getExtensionDelegate]
     */
    @SinceKotlin("1.1")
    public fun getDelegate(receiver: T): Any?

    override val getter: Getter<T, R>

    /**
     * Getter of the property is a `get` method declared alongside the property.
     *
     * Can be used as a function that takes an argument of type [T] (the receiver) and returns the value of the property type [R].
     */
    public interface Getter<T, out R> : KProperty.Getter<R>, (T) -> R
}

/**
 * Represents a `var`-property, operations on which take one receiver as a parameter.
 */
public actual interface KMutableProperty1<T, R> : KProperty1<T, R>, KMutableProperty<R> {
    /**
     * Modifies the value of the property.
     *
     * @param receiver the receiver which is used to modify the value of the property.
     *                 For example, it should be a class instance if this is a member property of that class,
     *                 or an extension receiver if this is a top level extension property.
     * @param value the new value to be assigned to this property.
     */
    public actual fun set(receiver: T, value: R)

    override val setter: Setter<T, R>

    /**
     * Setter of the property is a `set` method declared alongside the property.
     *
     * Can be used as a function that takes the receiver and the new property value as arguments and returns [Unit].
     */
    public interface Setter<T, R> : KMutableProperty.Setter<R>, (T, R) -> Unit
}


/**
 * Represents a property, operations on which take two receivers as parameters,
 * such as an extension property declared in a class.
 *
 * @param D the type of the first receiver. In case of the extension property in a class this is
 *        the type of the declaring class of the property, or any subclass of that class.
 * @param E the type of the second receiver. In case of the extension property in a class this is
 *        the type of the extension receiver.
 * @param R the type of the property.
 */
public actual interface KProperty2<D, E, out R> : KProperty<R>, (D, E) -> R {
    /**
     * Returns the current value of the property. In case of the extension property in a class,
     * the instance of the class should be passed first and the instance of the extension receiver second.
     *
     * @param receiver1 the instance of the first receiver.
     * @param receiver2 the instance of the second receiver.
     */
    public actual fun get(receiver1: D, receiver2: E): R

    /**
     * Returns the value of the delegate if this is a delegated property, or `null` if this property is not delegated.
     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/delegated-properties.html)
     * for more information.
     *
     * In case of the extension property in a class, the instance of the class should be passed first
     * and the instance of the extension receiver second.
     *
     * @param receiver1 the instance of the first receiver.
     * @param receiver2 the instance of the second receiver.
     *
     * @see [kotlin.reflect.full.getExtensionDelegate] // [KProperty2.getExtensionDelegate]
     */
    @SinceKotlin("1.1")
    public fun getDelegate(receiver1: D, receiver2: E): Any?

    override val getter: Getter<D, E, R>

    /**
     * Getter of the property is a `get` method declared alongside the property.
     *
     * Can be used as a function that takes an argument of type [D] (the first receiver), an argument of type [E] (the second receiver)
     * and returns the value of the property type [R].
     */
    public interface Getter<D, E, out R> : KProperty.Getter<R>, (D, E) -> R
}

/**
 * Represents a `var`-property, operations on which take two receivers as parameters.
 */
public actual interface KMutableProperty2<D, E, R> : KProperty2<D, E, R>, KMutableProperty<R> {
    /**
     * Modifies the value of the property.
     *
     * @param receiver1 the instance of the first receiver.
     * @param receiver2 the instance of the second receiver.
     * @param value the new value to be assigned to this property.
     */
    public actual fun set(receiver1: D, receiver2: E, value: R)

    override val setter: Setter<D, E, R>

    /**
     * Setter of the property is a `set` method declared alongside the property.
     *
     * Can be used as a function that takes an argument of type [D] (the first receiver), an argument of type [E] (the second receiver),
     * and the new property value and returns [Unit].
     */
    public interface Setter<D, E, R> : KMutableProperty.Setter<R>, (D, E, R) -> Unit
}
