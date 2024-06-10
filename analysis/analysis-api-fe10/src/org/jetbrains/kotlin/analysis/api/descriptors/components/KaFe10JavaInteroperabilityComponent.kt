/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaJavaInteroperabilityComponent
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getDescriptor
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.isTopLevel
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource

class KaFe10JavaInteroperabilityComponent(
    override val analysisSessionProvider: () -> KaFe10Session,
    override val token: KaLifetimeToken
) : KaSessionComponent<KaFe10Session>(), KaJavaInteroperabilityComponent {
    override val KaCallableSymbol.containingJvmClassName: String?
        get() = withValidityAssertion {
            with(analysisSession) {
                val platform = containingModule.platform
                if (!platform.has<JvmPlatform>()) return null

                val containingSymbolOrSelf = computeContainingSymbolOrSelf(this@containingJvmClassName, analysisSession)
                return when (val descriptor = containingSymbolOrSelf.getDescriptor()) {
                    is DescriptorWithContainerSource -> {
                        when (val containerSource = descriptor.containerSource) {
                            is FacadeClassSource -> containerSource.facadeClassName ?: containerSource.className
                            is KotlinJvmBinarySourceElement -> JvmClassName.byClassId(containerSource.binaryClass.classId)
                            else -> null
                        }?.fqNameForClassNameWithoutDollars?.asString()
                    }
                    else -> {
                        return if (containingSymbolOrSelf.isTopLevel) {
                            descriptor?.let(DescriptorToSourceUtils::getContainingFile)
                                ?.takeUnless { it.isScript() }
                                ?.javaFileFacadeFqName?.asString()
                        } else {
                            val classId = (containingSymbolOrSelf as? KaConstructorSymbol)?.containingClassId
                                ?: (containingSymbolOrSelf as? KaCallableSymbol)?.callableId?.classId
                            classId?.takeUnless { it.shortClassName.isSpecial }
                                ?.asFqNameString()
                        }
                    }
                }
            }
        }
}