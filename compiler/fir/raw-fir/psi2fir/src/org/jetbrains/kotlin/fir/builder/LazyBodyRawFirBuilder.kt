/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.containingClassAttr
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.isInner
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.psi.*

class RawFirFragmentForLazyBodiesBuilder private constructor(
    session: FirSession,
    baseScopeProvider: FirScopeProvider,
    private val declaration: KtDeclaration,
) : RawFirBuilder(session, baseScopeProvider, RawFirBuilderMode.NORMAL) {

    companion object {
        fun build(
            session: FirSession,
            baseScopeProvider: FirScopeProvider,
            designation: List<FirDeclaration>,
            declaration: KtDeclaration,
        ): FirDeclaration {
            val builder = RawFirFragmentForLazyBodiesBuilder(session, baseScopeProvider, declaration)
            builder.context.packageFqName = declaration.containingKtFile.packageFqName
            return builder.moveNext(designation.iterator())
        }
    }

    private fun moveNext(iterator: Iterator<FirDeclaration>): FirDeclaration {
        if (!iterator.hasNext()) {
            return if (declaration is KtProperty) {
                with(Visitor()) {
                    declaration.toFirProperty(null)
                }
            } else {
                declaration.accept(Visitor(), Unit) as FirDeclaration
            }
        }

        val parent = iterator.next()
        if (parent !is FirRegularClass) return moveNext(iterator)

        val classOrObject = parent.psi
        check(classOrObject is KtClassOrObject)

        withChildClassName(classOrObject.nameAsSafeName, false) {
            withCapturedTypeParameters {
                if (!parent.isInner) context.capturedTypeParameters = context.capturedTypeParameters.clear()
                addCapturedTypeParameters(parent.typeParameters.take(classOrObject.typeParameters.size))
                registerSelfType(classOrObject.toDelegatedSelfType(parent))
                return moveNext(iterator)
            }
        }
    }

    private fun PsiElement?.toDelegatedSelfType(firClass: FirRegularClass): FirResolvedTypeRef =
        toDelegatedSelfType(firClass.typeParameters, firClass.symbol)
}

