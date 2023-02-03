/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.util.IdSignature
import java.util.regex.Pattern

internal val functionPattern = Pattern.compile("^K?(Suspend)?Function\\d+$")

internal val functionTypeInterfacePackages = listOf("kotlin", "kotlin.coroutines", "kotlin.reflect")

fun checkIsFunctionTypeInterfacePackageFqName(fqName: String) = fqName in functionTypeInterfacePackages

fun checkIsFunctionInterface(idSig: IdSignature?): Boolean {
    val publicSig = idSig?.asPublic()
    return publicSig != null &&
            checkIsFunctionTypeInterfacePackageFqName(publicSig.packageFqName) &&
            publicSig.declarationFqName.isNotEmpty() &&
            functionPattern.matcher(publicSig.firstNameSegment).find()
}
