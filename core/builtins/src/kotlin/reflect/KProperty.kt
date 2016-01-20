/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect

/**
 * Represents a property, such as a named `val` or `var` declaration.
 * Instances of this class are obtainable by the `::` operator.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/reflection.html)
 * for more information.
 *
 * @param R the type of the property.
 */
public interface KProperty<out R> : KCallable<R> {
    /** The getter of this property, used to obtain the value of the property. */
    public val getter: Getter<R>

    /**
     * Represents a property accessor, which is a `get` or `set` method declared alongside the property.
     * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/properties.html#getters-and-setters)
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
public interface KMutableProperty<R> : KProperty<R> {
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
public interface KProperty0<out R> : KProperty<R>, () -> R {
    /**
     * Returns the current value of the property.
     */
    public fun get(): R

    override val getter: Getter<R>

    public interface Getter<out R> : KProperty.Getter<R>, () -> R
}

/**
 * Represents a `var`-property without any kind of receiver.
 */
public interface KMutableProperty0<R> : KProperty0<R>, KMutableProperty<R> {
    /**
     * Modifies the value of the property.
     *
     * @param value the new value to be assigned to this property.
     */
    public fun set(value: R)

    override val setter: Setter<R>

    public interface Setter<R> : KMutableProperty.Setter<R>, (R) -> Unit
}


/**
 * Represents a property, operations on which take one receiver as a parameter.
 *
 * @param T the type of the receiver which should be used to obtain the value of the property.
 * @param R the type of the property.
 */
public interface KProperty1<T, out R> : KProperty<R>, (T) -> R {
    /**
     * Returns the current value of the property.
     *
     * @param receiver the receiver which is used to obtain the value of the property.
     *                 For example, it should be a class instance if this is a member property of that class,
     *                 or an extension receiver if this is a top level extension property.
     */
    public fun get(receiver: T): R

    override val getter: Getter<T, R>

    public interface Getter<T, out R> : KProperty.Getter<R>, (T) -> R
}

/**
 * Represents a `var`-property, operations on which take one receiver as a parameter.
 */
public interface KMutableProperty1<T, R> : KProperty1<T, R>, KMutableProperty<R> {
    /**
     * Modifies the value of the property.
     *
     * @param receiver the receiver which is used to modify the value of the property.
     *                 For example, it should be a class instance if this is a member property of that class,
     *                 or an extension receiver if this is a top level extension property.
     * @param value the new value to be assigned to this property.
     */
    public fun set(receiver: T, value: R)

    override val setter: Setter<T, R>

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
public interface KProperty2<D, E, out R> : KProperty<R>, (D, E) -> R {
    /**
     * Returns the current value of the property. In case of the extension property in a class,
     * the instance of the class should be passed first and the instance of the extension receiver second.
     *
     * @param receiver1 the instance of the first receiver.
     * @param receiver2 the instance of the second receiver.
     */
    public fun get(receiver1: D, receiver2: E): R

    override val getter: Getter<D, E, R>

    public interface Getter<D, E, out R> : KProperty.Getter<R>, (D, E) -> R
}

/**
 * Represents a `var`-property, operations on which take two receivers as parameters.
 */
public interface KMutableProperty2<D, E, R> : KProperty2<D, E, R>, KMutableProperty<R> {
    /**
     * Modifies the value of the property.
     *
     * @param receiver1 the instance of the first receiver.
     * @param receiver2 the instance of the second receiver.
     * @param value the new value to be assigned to this property.
     */
    public fun set(receiver1: D, receiver2: E, value: R)

    override val setter: Setter<D, E, R>

    public interface Setter<D, E, R> : KMutableProperty.Setter<R>, (D, E, R) -> Unit
}
