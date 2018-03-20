/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.types.ConeAbbreviatedType
import org.jetbrains.kotlin.fir.types.ConeClassType
import org.jetbrains.kotlin.fir.types.FirResolvedType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirNestedClassifierScope(val classId: ClassId, val session: FirSession) : FirScope {

    private val firProvider = FirProvider.getInstance(session)

    fun ClassId.getFir(): FirMemberDeclaration? {
        return firProvider.getFirClassifierByFqName(this)
    }

    private tailrec fun ConeClassType.computePartialExpansion(): ClassId {
        return when (this) {
            !is ConeAbbreviatedType -> this.fqName
            else -> (this.directExpansion as ConeClassType).computePartialExpansion()
        }
    }

    private val superScopes by lazy {
        val self = classId.getFir()
        when (self) {
            is FirClass -> {
                val superTypes = self.superTypes as List<FirResolvedType>
                FirCompositeScope(superTypes.mapTo(ArrayList(superTypes.size)) {
                    FirNestedClassifierScope(it.coneTypeUnsafe<ConeClassType>().computePartialExpansion(), session)
                })
            }
            is FirTypeAlias -> {
                val expansionTarget = self.abbreviatedType.coneTypeUnsafe<ConeClassType>().computePartialExpansion()
                FirNestedClassifierScope(expansionTarget, session)
            }
            else -> error("!")
        }
    }

    override fun processClassifiersByName(name: Name, processor: (ClassId) -> Boolean): Boolean {
        val child = ClassId(classId.packageFqName, classId.relativeClassName.child(name), false)
        if (child.getFir() != null && !processor(child)) return false

        return superScopes.processClassifiersByName(name, processor)
    }


}
