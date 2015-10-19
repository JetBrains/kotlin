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

package org.jetbrains.kotlin.renderer

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.JetType
import java.lang.reflect.Modifier
import kotlin.properties.Delegates
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KType

internal class DescriptorRendererOptionsImpl : DescriptorRendererOptions {
    public var isLocked: Boolean = false
        private set

    public fun lock() {
        assert(!isLocked)
        isLocked = true
    }

    public fun copy(): DescriptorRendererOptionsImpl {
        val copy = DescriptorRendererOptionsImpl()

        //TODO: use Kotlin reflection
        for (field in this.javaClass.declaredFields) {
            if (field.modifiers.and(Modifier.STATIC) != 0) continue
            field.isAccessible = true
            val property = field.get(this) as? ObservableProperty<*> ?: continue
            val value = property.getValue(this, object : KProperty<Any?>, PropertyMetadata {
                override val parameters: List<KParameter>
                    get() = error("Should not be called")
                override val returnType: KType
                    get() = error("Should not be called")
                override val getter: KProperty.Getter<Any?>
                    get() = error("Should not be called")
                override val annotations: List<Annotation>
                    get() = error("Should not be called")

                override fun call(vararg args: Any?): Any? = error("Should not be called")

                override fun callBy(args: Map<KParameter, Any?>): Any? = error("Should not be called")

                override val name = "" /* not used */
            })
            field.set(copy, copy.property(value as Any))
        }

        return copy
    }

    private fun <T> property(initialValue: T): ReadWriteProperty<DescriptorRendererOptionsImpl, T> {
        return Delegates.vetoable(initialValue) { property, oldValue, newValue ->
            if (isLocked) {
                throw IllegalStateException("Cannot modify readonly DescriptorRendererOptions")
            }
            else {
                true
            }
        }
    }

    override var nameShortness by property(NameShortness.SOURCE_CODE_QUALIFIED)
    override var withDefinedIn by property(true)
    override var modifiers: Set<DescriptorRendererModifier> by property(DescriptorRendererModifier.DEFAULTS)
    override var startFromName by property(false)
    override var debugMode by property(false)
    override var classWithPrimaryConstructor by property(false)
    override var verbose by property(false)
    override var unitReturnType by property(true)
    override var withoutReturnType by property(false)
    override var normalizedVisibilities by property(false)
    override var showInternalKeyword by property(true)
    override var prettyFunctionTypes by property(true)
    override var uninferredTypeParameterAsName by property(false)
    override var includePropertyConstant by property(false)
    override var withoutTypeParameters by property(false)
    override var withoutSuperTypes by property(false)
    override var typeNormalizer by property<(JetType) -> JetType>({ it })
    override var renderDefaultValues by property(true)
    override var flexibleTypesForCode by property(false)
    override var secondaryConstructorsAsPrimary by property(true)
    override var overrideRenderingPolicy by property(OverrideRenderingPolicy.RENDER_OPEN)
    override var valueParametersHandler: DescriptorRenderer.ValueParametersHandler by property(DescriptorRenderer.ValueParametersHandler.DEFAULT)
    override var textFormat by property(RenderingFormat.PLAIN)
    override var parameterNameRenderingPolicy by property(ParameterNameRenderingPolicy.ALL)
    override var receiverAfterName by property(false)
    override var renderCompanionObjectName by property(false)
    override var renderAccessors by property(false)
    override var renderDefaultAnnotationArguments by property(false)

    override var excludedAnnotationClasses by property(emptySet<FqName>())

    override var excludedTypeAnnotationClasses by property(setOf(
            FqName("org.jetbrains.annotations.ReadOnly"),
            FqName("org.jetbrains.annotations.Mutable"),
            FqName("org.jetbrains.annotations.NotNull"),
            FqName("org.jetbrains.annotations.Nullable"),
            FqName("kotlin.internal.NoInfer"),
            FqName("kotlin.internal.Exact")
    ))
}