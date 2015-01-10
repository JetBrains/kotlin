/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.name

import org.jetbrains.jet.lang.resolve.ImportPath

public fun FqName.isSubpackageOf(packageName: FqName): Boolean {
    return when {
        this == packageName -> true
        packageName.isRoot() -> true
        else -> isSubpackageOf(this.asString(), packageName.asString())
    }
}

public fun FqName.isParent(child: FqName): Boolean = child.isSubpackageOf(this)

public fun FqName.isOneSegmentFQN(): Boolean = !isRoot() && parent().isRoot()

public fun FqName.withoutFirstSegment(): FqName {
    if (isRoot() || parent().isRoot()) return FqName.ROOT

    val fqNameStr = asString()
    return FqName(fqNameStr.substring(fqNameStr.indexOf('.') + 1, fqNameStr.length()))
}

public fun FqName.numberOfSegments(): Int {
    return if (isRoot()) 0 else 1 + parent().numberOfSegments()
}

/**
 * Get tail part of the full fqn by subtracting head part.
 *
 * @param headFQN
 * @return tail fqn. If first part is not a begging of the full fqn, fullFQN will be returned.
 */
public fun FqName.tail(headFQN: FqName): FqName {
    return when {
        !isSubpackageOf(headFQN) || headFQN.isRoot() -> this
        this == headFQN -> FqName.ROOT
        else -> FqName(asString().substring(headFQN.asString().length + 1))
    }
}

public fun FqName.isImported(importPath: ImportPath, skipAliasedImports: Boolean = true): Boolean {
    return when {
        skipAliasedImports && importPath.hasAlias() -> false
        importPath.isAllUnder() && !isRoot() -> importPath.fqnPart() == this.parent()
        else -> importPath.fqnPart() == this
    }
}

public fun ImportPath.isImported(alreadyImported: ImportPath): Boolean {
    return if (isAllUnder() || hasAlias()) this == alreadyImported else fqnPart().isImported(alreadyImported)
}

public fun ImportPath.isImported(imports: Iterable<ImportPath>): Boolean = imports.any { isImported(it) }

// Check that it is javaName(\.javaName)* or an empty string
private enum class State {
    BEGINNING
    MIDDLE
    AFTER_DOT
}

public fun isValidJavaFqName(qualifiedName: String?): Boolean {
    if (qualifiedName == null) return false

    var state = State.BEGINNING

    for (c in qualifiedName) {
        when (state) {
            State.BEGINNING, State.AFTER_DOT -> {
                if (!Character.isJavaIdentifierPart(c)) return false
                state = State.MIDDLE
            }
            State.MIDDLE -> {
                if (c == '.') {
                    state = State.AFTER_DOT
                }
                else if (!Character.isJavaIdentifierPart(c)) return false
            }
        }
    }

    return state != State.AFTER_DOT
}

public fun FqName.getFirstSegment(): Name = this.pathSegments().first()

tailRecursive
public fun FqName.each(operation: (FqName) -> Boolean) {
    if (operation(this) && !isRoot()) {
        parent().each(operation)
    }
}

private fun isSubpackageOf(subpackageNameStr: String, packageNameStr: String): Boolean {
    return subpackageNameStr == packageNameStr ||
        (subpackageNameStr.startsWith(packageNameStr) && subpackageNameStr[packageNameStr.length()] == '.')
}

private fun getFirstSegment(fqn: String): String {
    val dotIndex = fqn.indexOf('.')
    return if ((dotIndex != -1)) fqn.substring(0, dotIndex) else fqn
}
