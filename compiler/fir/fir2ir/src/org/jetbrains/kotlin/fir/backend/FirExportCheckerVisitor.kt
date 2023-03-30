/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinExportChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.SpecialDeclarationType
import org.jetbrains.kotlin.backend.common.serialization.mangle.ir.isAnonymous
import org.jetbrains.kotlin.backend.common.serialization.mangle.publishedApiAnnotation
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.firClassLike
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.ClassId

abstract class FirExportCheckerVisitor : FirVisitor<Boolean, SpecialDeclarationType>(), KotlinExportChecker<FirDeclaration> {
    override fun check(declaration: FirDeclaration, type: SpecialDeclarationType): Boolean =
        declaration.accept(this, type)

    override fun visitElement(element: FirElement, data: SpecialDeclarationType): Boolean =
        TODO("Should have not been reached")

    private fun <D> D.globalMemberIsExported(): Boolean where D : FirMemberDeclaration {
        val visibility = visibility
        if (visibility.isPublicAPI || visibility === Visibilities.Internal) return true
        if (visibility === Visibilities.Local) return false
        return annotations.hasAnnotation(ClassId.topLevel(publishedApiAnnotation), moduleData.session) || isPlatformSpecificExported()
    }

    private fun <D> D.isExported(): Boolean where D : FirCallableDeclaration {
        val classId = symbol.callableId.classId ?: return globalMemberIsExported()
        return visibility !== Visibilities.Local &&
                classId.toSymbol(moduleData.session)!!.fir.accept(this@FirExportCheckerVisitor, SpecialDeclarationType.REGULAR)
    }

    private fun <D> D.isExported(): Boolean where D : FirClassLikeDeclaration {
        val containingDeclaration = getContainingDeclaration(moduleData.session) ?: return globalMemberIsExported()
        return visibility !== Visibilities.Local &&
                containingDeclaration.accept(this@FirExportCheckerVisitor, SpecialDeclarationType.REGULAR)
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: SpecialDeclarationType): Boolean =
        !simpleFunction.name.isAnonymous && simpleFunction.isExported()

    override fun visitRegularClass(regularClass: FirRegularClass, data: SpecialDeclarationType): Boolean {
        if (data == SpecialDeclarationType.ANON_INIT) return false
        if (regularClass.name.isAnonymous) return false
        return regularClass.isExported()
    }

    override fun visitConstructor(constructor: FirConstructor, data: SpecialDeclarationType): Boolean {
        return constructor.returnTypeRef.firClassLike(constructor.moduleData.session)!!.isExported()
    }

    override fun visitProperty(property: FirProperty, data: SpecialDeclarationType): Boolean = property.isExported()

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: SpecialDeclarationType): Boolean = false

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: SpecialDeclarationType): Boolean = false
}
