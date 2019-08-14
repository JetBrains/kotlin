/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.utils.JavaTypeEnhancementState
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class AnalysisFlag<out T> internal constructor(
    private val name: String,
    val defaultValue: T
) {
    override fun equals(other: Any?): Boolean = other is AnalysisFlag<*> && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    class Delegate<out T>(name: String, defaultValue: T) : ReadOnlyProperty<Any?, AnalysisFlag<T>> {
        private val flag = AnalysisFlag(name, defaultValue)

        override fun getValue(thisRef: Any?, property: KProperty<*>): AnalysisFlag<T> = flag
    }

    object Delegates {
        object Boolean {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Delegate(property.name, false)
        }

        object ApiModeDisabledByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Delegate(property.name, ExplicitApiMode.DISABLED)
        }

        object Jsr305StateWarnByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Delegate(property.name, JavaTypeEnhancementState.DEFAULT)
        }

        object JvmDefaultModeDisabledByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Delegate(property.name, JvmDefaultMode.DISABLE)
        }

        object ListOfStrings {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Delegate(property.name, emptyList<String>())
        }

        object ConstraintSystemForOverloadResolution {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) =
                Delegate(property.name, ConstraintSystemForOverloadResolutionMode.CONSTRAINT_SYSTEM_FOR_NEW_INFERENCE)
        }
    }
}
