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

package kotlin.reflect.jvm

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.jvm.internal.Reflection
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.*

// Kotlin reflection -> Java reflection

public val <T> KClass<T>.java: Class<T>
    get() = (this as KClassImpl<T>).jClass

public val KPackage.javaFacade: Class<*>
    get() = (this as KPackageImpl).jClass


public val KProperty<*>.javaGetter: Method?
    get() = (this as? KPropertyImpl<*>)?.getter

public val KMutableProperty<*>.javaSetter: Method?
    get() = (this as? KMutablePropertyImpl<*>)?.setter


public val KTopLevelVariable<*>.javaField: Field?
    get() = (this as KPropertyImpl<*>).field

public val KTopLevelVariable<*>.javaGetter: Method
    get() = (this as KTopLevelVariableImpl<*>).getter

public val KMutableTopLevelVariable<*>.javaSetter: Method
    get() = (this as KMutableTopLevelVariableImpl<*>).setter


public val KTopLevelExtensionProperty<*, *>.javaGetter: Method
    get() = (this as KTopLevelExtensionPropertyImpl<*, *>).getter

public val KMutableTopLevelExtensionProperty<*, *>.javaSetter: Method
    get() = (this as KMutableTopLevelExtensionPropertyImpl<*, *>).setter


public val KMemberProperty<*, *>.javaField: Field?
    get() = (this as KPropertyImpl<*>).field


// Java reflection -> Kotlin reflection

// TODO: getstatic $kotlinClass or go to foreignKClasses
public val <T> Class<T>.kotlin: KClass<T>
    get() = KClassImpl(this)

// TODO: getstatic $kotlinPackage
public val Class<*>.kotlinPackage: KPackage
    get() = KPackageImpl(this)


public val Field.kotlin: KProperty<*>
    get() {
        val clazz = getDeclaringClass()
        val name = getName()
        val modifiers = getModifiers()
        val static = Modifier.isStatic(modifiers)
        val final = Modifier.isFinal(modifiers)
        if (static) {
            val kPackage = clazz.kotlinPackage
            return if (final) Reflection.topLevelVariable(name, kPackage) else Reflection.mutableTopLevelVariable(name, kPackage)
        }
        else {
            val kClass = (clazz as Class<Any>).kotlin
            return if (final) Reflection.memberProperty(name, kClass) else Reflection.mutableMemberProperty(name, kClass)
        }
    }
