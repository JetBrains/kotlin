/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.name.Name

public sealed interface KaClassTypeQualifier : KaLifetimeOwner {
    public val name: Name
    public val typeArguments: List<KaTypeProjection>
}

public interface KaResolvedClassTypeQualifier : KaClassTypeQualifier {
    public val symbol: KaClassifierSymbol
}

public interface KaUnresolvedClassTypeQualifier : KaClassTypeQualifier

@Deprecated("Use 'KaClassTypeQualifier' instead", ReplaceWith("KaClassTypeQualifier"))
public typealias KtClassTypeQualifier = KaClassTypeQualifier
