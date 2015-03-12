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

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.JetElementImplStub
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubWithFqName
import java.lang.reflect.Method
import java.util.ArrayList

public open class KotlinStubBaseImpl<T : JetElementImplStub<*>>(parent: StubElement<*>?, elementType: IStubElementType<*, *>) : StubBase<T>(parent, elementType) {

    override fun toString(): String {
        val stubInterface = this.javaClass.getInterfaces().filter { it.getName().endsWith("Stub") }.single()
        val propertiesValues = renderPropertyValues(stubInterface)
        if (propertiesValues.isEmpty()) {
            return ""
        }
        return propertiesValues.join(separator = ", ", prefix = "[", postfix = "]")
    }

    private fun renderPropertyValues(stubInterface: Class<out Any?>): List<String> {
        return collectProperties(stubInterface).map { property -> renderProperty(property) }.filterNotNull().sort()
    }

    private fun collectProperties(stubInterface: Class<*>): Collection<Method> {
        val result = ArrayList<Method>()
        result.addAll(stubInterface.getDeclaredMethods().filter { it.getParameterTypes()!!.isEmpty() })
        for (baseInterface in stubInterface.getInterfaces()) {
            if (baseInterface in BASE_STUB_INTERFACES) {
                result.addAll(collectProperties(baseInterface))
            }
        }
        return result
    }

    private fun renderProperty(property: Method): String? {
        return try {
            val value = property.invoke(this)
            val name = getPropertyName(property)
            "$name=$value"
        }
        catch (e: Exception) {
            LOGGER.error(e)
            null
        }
    }

    private fun getPropertyName(method: Method): String {
        val methodName = method.getName()!!
        if (methodName.startsWith("get")) {
            return methodName.substring(3).decapitalize()
        }
        return methodName
    }

    default object {
        private val LOGGER: Logger = Logger.getInstance(javaClass<KotlinStubBaseImpl<JetElementImplStub<*>>>())

        private val BASE_STUB_INTERFACES = listOf(javaClass<KotlinStubWithFqName<*>>(), javaClass<KotlinClassOrObjectStub<*>>(), javaClass<NamedStub<*>>())
    }
}
