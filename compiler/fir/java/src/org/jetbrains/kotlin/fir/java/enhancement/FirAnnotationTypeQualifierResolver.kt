/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.load.java.AbstractAnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME
import org.jetbrains.kotlin.name.FqName

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
class FirAnnotationTypeQualifierResolver(private val session: FirSession) :
    AbstractAnnotationTypeQualifierResolver<FirAnnotationCall>(session.javaTypeEnhancementState)
{
    override val FirAnnotationCall.annotations: Iterable<FirAnnotationCall>
        get() = coneClassLikeType?.lookupTag?.toSymbol(session)?.fir?.annotations.orEmpty()

    override val FirAnnotationCall.key: Any
        get() = coneClassLikeType!!.lookupTag

    override val FirAnnotationCall.fqName: FqName?
        get() = coneClassLikeType?.lookupTag?.classId?.asSingleFqName()

    override fun FirAnnotationCall.enumArguments(onlyValue: Boolean): Iterable<String> =
        arguments.flatMap { argument ->
            if (!onlyValue || argument !is FirNamedArgumentExpression || argument.name == DEFAULT_ANNOTATION_MEMBER_NAME)
                argument.toEnumNames()
            else
                emptyList()
        }

    private fun FirExpression.toEnumNames(): List<String> =
        when (this) {
            is FirArrayOfCall -> arguments.flatMap { it.toEnumNames() }
            else -> listOfNotNull(toResolvedCallableSymbol()?.callableId?.callableName?.asString())
        }
}
