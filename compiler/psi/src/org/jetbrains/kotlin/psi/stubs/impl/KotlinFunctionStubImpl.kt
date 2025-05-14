/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.contracts.description.KtContractDescriptionElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import java.io.IOException

class KotlinFunctionStubImpl(
    parent: StubElement<out PsiElement>?,
    private val nameRef: StringRef?,
    private val isTopLevel: Boolean,
    private val fqName: FqName?,
    private val isExtension: Boolean,
    private val isStaticExtension: Boolean,
    private val hasBlockBody: Boolean,
    private val hasBody: Boolean,
    private val hasTypeParameterListBeforeFunctionName: Boolean,
    private val mayHaveContract: Boolean,
    val contract: List<KtContractDescriptionElement<KotlinTypeBean, Nothing?>>?,
    val origin: KotlinStubOrigin?
) : KotlinStubBaseImpl<KtNamedFunction>(parent, KtStubElementTypes.FUNCTION), KotlinFunctionStub {
    init {
        if (isTopLevel && fqName == null) {
            throw IllegalArgumentException("fqName shouldn't be null for top level functions")
        }
    }

    override fun getFqName() = fqName

    override fun getName() = StringRef.toString(nameRef)
    override fun isTopLevel() = isTopLevel
    override fun isExtension() = isExtension
    override fun isStaticExtension(): Boolean = isStaticExtension
    override fun hasBlockBody() = hasBlockBody
    override fun hasBody() = hasBody
    override fun hasTypeParameterListBeforeFunctionName() = hasTypeParameterListBeforeFunctionName
    override fun mayHaveContract(): Boolean = mayHaveContract

    @Throws(IOException::class)
    fun serializeContract(dataStream: StubOutputStream) {
        val effects: List<KtContractDescriptionElement<KotlinTypeBean, Nothing?>>? = contract
        dataStream.writeVarInt(effects?.size ?: 0)
        val visitor = KotlinContractSerializationVisitor(dataStream)
        effects?.forEach { it.accept(visitor, null) }
    }

    companion object {
        fun deserializeContract(dataStream: StubInputStream): List<KtContractDescriptionElement<KotlinTypeBean, Nothing?>> {
            val effects = mutableListOf<KtContractDescriptionElement<KotlinTypeBean, Nothing?>>()
            val count: Int = dataStream.readVarInt()
            for (i in 0 until count) {
                val effectType: KotlinContractEffectType = KotlinContractEffectType.entries[dataStream.readVarInt()]
                effects.add(effectType.deserialize(dataStream))
            }
            return effects
        }
    }
}
