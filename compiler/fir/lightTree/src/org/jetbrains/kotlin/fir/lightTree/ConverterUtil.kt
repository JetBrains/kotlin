/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirDelegatedConstructorCallImpl
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.expressions.impl.FirReturnExpressionImpl
import org.jetbrains.kotlin.fir.lightTree.fir.ValueParameter
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.MemberModifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.PlatformModifier
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirQualifierPartImpl
import org.jetbrains.kotlin.fir.types.impl.FirUserTypeRefImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

object ConverterUtil {
    fun String?.nameAsSafeName(defaultName: String = ""): Name {
        return when {
            this != null -> Name.identifier(this)
            defaultName.isNotEmpty() -> Name.identifier(defaultName)
            else -> SpecialNames.NO_NAME_PROVIDED
        }
    }

    fun Name?.toDelegatedSelfType(session: FirSession): FirTypeRef =
        FirUserTypeRefImpl(session, null, isNullable = false).apply {
            qualifier.add(
                FirQualifierPartImpl(
                    this@toDelegatedSelfType ?: SpecialNames.NO_NAME_PROVIDED
                )
            )
        }

    fun LighterASTNode?.hasSecondaryConstructor(tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
        if (this == null) return false
        //TODO check if node isn't CLASS_BODY
        val kidsRef = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, kidsRef)
        val kidsArray = kidsRef.get()

        for (kid in kidsArray) {
            if (kid == null) continue
            when (kid.tokenType) {
                KtNodeTypes.SECONDARY_CONSTRUCTOR -> return true
            }
        }
        return false
    }

    fun ValueParameter.toFirProperty(): FirProperty {
        val modifier = this.modifier
        val name = this.firValueParameter.name
        val type = this.firValueParameter.returnTypeRef

        return FirMemberPropertyImpl(
            this.firValueParameter.session,
            null,
            FirPropertySymbol(ClassNameUtil.callableIdForName(name)),
            name,
            modifier.visibilityModifier.toVisibility(),
            modifier.inheritanceModifier?.toModality(),
            modifier.platformModifier == PlatformModifier.EXPECT,
            modifier.platformModifier == PlatformModifier.ACTUAL,
            isOverride = modifier.memberModifier == MemberModifier.OVERRIDE,
            isConst = false,
            isLateInit = false,
            receiverTypeRef = null,
            returnTypeRef = type,
            isVar = this.isVar,
            initializer = null,
            getter = FirDefaultPropertyGetter(this.firValueParameter.session, null, type, modifier.visibilityModifier.toVisibility()),
            setter = FirDefaultPropertySetter(this.firValueParameter.session, null, type, modifier.visibilityModifier.toVisibility()),
            delegate = null
        ).apply { annotations += this@toFirProperty.firValueParameter.annotations }
    }

    fun FirExpression.toReturn(labelName: String? = null): FirReturnExpression {
        return FirReturnExpressionImpl(
            session,
            null,
            this
        ).apply {
            target = FirFunctionTarget(labelName)
            val lastFunction = FunctionUtil.firFunctions.lastOrNull()
            if (labelName == null) {
                if (lastFunction != null) {
                    target.bind(lastFunction)
                } else {
                    target.bind(FirErrorFunction(session, psi, "Cannot bind unlabeled return to a function"))
                }
            } else {
                for (firFunction in FunctionUtil.firFunctions.asReversed()) {
                    when (firFunction) {
                        is FirAnonymousFunction -> {
                            if (firFunction.label?.name == labelName) {
                                target.bind(firFunction)
                                return@apply
                            }
                        }
                        is FirNamedFunction -> {
                            if (firFunction.name.asString() == labelName) {
                                target.bind(firFunction)
                                return@apply
                            }
                        }
                    }
                }
                target.bind(FirErrorFunction(session, psi, "Cannot bind label $labelName to a function"))
            }
        }
    }

    fun FirValueParameter.toFirExpression(session: FirSession, stubMode: Boolean): FirExpression {
        return if (stubMode) FirExpressionStub(session, null)
        else TODO("not implemeted")
    }
}

object ClassNameUtil {
    lateinit var packageFqName: FqName

    inline fun <T> withChildClassName(name: Name, l: () -> T): T {
        className = className.child(name)
        val t = l()
        className = className.parent()
        return t
    }

    val currentClassId
        get() = ClassId(packageFqName, className, false)

    fun callableIdForName(name: Name) =
        if (className == FqName.ROOT) CallableId(packageFqName, name)
        else CallableId(packageFqName, className, name)

    fun callableIdForClassConstructor() =
        if (className == FqName.ROOT) CallableId(packageFqName, Name.special("<anonymous-init>"))
        else CallableId(packageFqName, className, className.shortName())

    var className: FqName = FqName.ROOT
}

object FunctionUtil {
    val firFunctions = mutableListOf<FirFunction>()

    fun <T> MutableList<T>.removeLast() {
        removeAt(size - 1)
    }

    fun <T> MutableList<T>.pop(): T? {
        val result = lastOrNull()
        if (result != null) {
            removeAt(size - 1)
        }
        return result
    }
}


