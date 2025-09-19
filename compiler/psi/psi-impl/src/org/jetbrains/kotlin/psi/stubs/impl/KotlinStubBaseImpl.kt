/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtElementImplStub
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.*
import java.lang.reflect.Method

val STUB_TO_STRING_PREFIX = "KotlinStub$"

@OptIn(KtImplementationDetail::class)
abstract class KotlinStubBaseImpl<T : KtElementImplStub<*>>(parent: StubElement<*>?, elementType: IStubElementType<*, *>) :
    StubBase<T>(parent, elementType), KotlinStubElement<T> {

    @KtImplementationDetail
    abstract override fun copyInto(newParent: StubElement<*>?): KotlinStubBaseImpl<T>

    @Deprecated("Deprecated stub API")
    @Suppress("DEPRECATION") // KT-78356
    override fun getStubType(): IStubElementType<out StubElement<*>, *> =
        super.getStubType() as IStubElementType<out StubElement<*>, *>

    override fun toString(): String {
        val stubInterface = this::class.java.interfaces.single { it.name.contains("Stub") }
        val propertiesValues = renderPropertyValues(stubInterface)
        if (propertiesValues.isEmpty()) {
            @Suppress("DEPRECATION") // KT-78356
            return "$STUB_TO_STRING_PREFIX$stubType"
        }
        val properties = propertiesValues.joinToString(separator = ", ", prefix = "[", postfix = "]")
        @Suppress("DEPRECATION") // KT-78356
        return "$STUB_TO_STRING_PREFIX$stubType$properties"
    }

    private fun renderPropertyValues(stubInterface: Class<out Any?>): List<String> {
        return collectProperties(stubInterface).mapNotNull { property -> renderProperty(property) }.sorted()
    }

    private fun collectProperties(stubInterface: Class<*>): Collection<Method> = buildList {
        stubInterface.declaredMethods.filterTo(this) { it.parameterTypes.isEmpty() }
        for (baseInterface in stubInterface.interfaces) {
            if (baseInterface in BASE_STUB_INTERFACES) {
                this += collectProperties(baseInterface)
            }
        }
    }

    private fun renderProperty(property: Method): String? {
        return try {
            val value = property.invoke(this)
            val name = getPropertyName(property)
            "$name=$value"
        } catch (e: Exception) {
            LOGGER.error(e)
            null
        }
    }

    private fun getPropertyName(method: Method): String {
        val methodName = method.name
        if (methodName.startsWith("get")) {
            return methodName.substring(3).replaceFirstChar(Char::lowercaseChar)
        }
        return methodName
    }

    companion object {
        private val LOGGER: Logger = Logger.getInstance(KotlinStubBaseImpl::class.java)

        private val BASE_STUB_INTERFACES = listOf(
            KotlinStubWithFqName::class.java,
            KotlinClassifierStub::class.java,
            KotlinClassOrObjectStub::class.java,
            NamedStub::class.java,
            KotlinCallableStubBase::class.java,
            KotlinPlaceHolderWithTextStub::class.java,
            KotlinDeclarationWithBodyStub::class.java,
        )
    }
}
