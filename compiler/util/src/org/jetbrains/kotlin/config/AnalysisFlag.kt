/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.utils.Jsr305State
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class AnalysisFlag<out T> internal constructor(
        private val name: String,
        val defaultValue: T
) {
    override fun equals(other: Any?): Boolean = other is AnalysisFlag<*> && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = name

    private class Flag<out T>(name: String, defaultValue: T) : ReadOnlyProperty<Any?, AnalysisFlag<T>> {
        private val flag = AnalysisFlag(name, defaultValue)

        override fun getValue(thisRef: Any?, property: KProperty<*>): AnalysisFlag<T> = flag

        object Boolean {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Flag(property.name, false)
        }

        object Jsr305StateWarnByDefault {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Flag(property.name, Jsr305State.DEFAULT)
        }

        object JvmDefaultModeDisabledByDefaul {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Flag(property.name, org.jetbrains.kotlin.config.JvmDefaultMode.DISABLE)
        }

        object ListOfStrings {
            operator fun provideDelegate(instance: Any?, property: KProperty<*>) = Flag(property.name, emptyList<String>())
        }
    }

    companion object Flags {
        @JvmStatic
        val skipMetadataVersionCheck by Flag.Boolean

        @JvmStatic
        val strictMetadataVersionSemantics by Flag.Boolean

        @JvmStatic
        val multiPlatformDoNotCheckActual by Flag.Boolean

        @JvmStatic
        val jsr305 by Flag.Jsr305StateWarnByDefault

        @JvmStatic
        val allowKotlinPackage by Flag.Boolean

        @JvmStatic
        val experimental by Flag.ListOfStrings

        @JvmStatic
        val useExperimental by Flag.ListOfStrings

        @JvmStatic
        val explicitApiVersion by Flag.Boolean

        @JvmStatic
        val ignoreDataFlowInAssert by Flag.Boolean

        @JvmStatic
        val jvmDefaultMode by Flag.JvmDefaultModeDisabledByDefaul

        @JvmStatic
        val allowResultReturnType by Flag.Boolean
    }
}
