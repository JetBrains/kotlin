/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.fir.declarations.resolved
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.name.Name

internal fun mapAnnotationParameters(annotation: FirAnnotation): Map<Name, FirExpression> {
    assert(annotation.resolved) { "By now the annotations argument mapping should have been resolved" }
    return annotation.argumentMapping.mapping.mapKeys { (name, _) -> name }
}
