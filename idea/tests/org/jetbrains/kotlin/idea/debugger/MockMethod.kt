/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.sun.jdi.Location
import com.sun.jdi.Method

class MockMethod : Method {
    override fun name() = ""
    override fun allLineLocations() = emptyList<Location>()

    override fun isSynthetic() = throw UnsupportedOperationException()
    override fun isFinal() = throw UnsupportedOperationException()
    override fun isStatic() = throw UnsupportedOperationException()
    override fun declaringType() = throw UnsupportedOperationException()
    override fun signature() = throw UnsupportedOperationException()
    override fun genericSignature() = throw UnsupportedOperationException()
    override fun variables() = throw UnsupportedOperationException()
    override fun variablesByName(name: String?) = throw UnsupportedOperationException()
    override fun bytecodes() = throw UnsupportedOperationException()
    override fun isBridge() = throw UnsupportedOperationException()
    override fun isObsolete() = throw UnsupportedOperationException()
    override fun isSynchronized() = throw UnsupportedOperationException()
    override fun allLineLocations(stratum: String?, sourceName: String?) = throw UnsupportedOperationException()
    override fun isNative() = throw UnsupportedOperationException()
    override fun locationOfCodeIndex(codeIndex: Long) = throw UnsupportedOperationException()
    override fun arguments() = throw UnsupportedOperationException()
    override fun isAbstract() = throw UnsupportedOperationException()
    override fun isVarArgs() = throw UnsupportedOperationException()
    override fun returnTypeName() = throw UnsupportedOperationException()
    override fun argumentTypes( ) = throw UnsupportedOperationException()
    override fun isConstructor() = throw UnsupportedOperationException()
    override fun locationsOfLine(lineNumber: Int) = throw UnsupportedOperationException()
    override fun locationsOfLine(stratum: String?, sourceName: String?, lineNumber: Int) = throw UnsupportedOperationException()
    override fun argumentTypeNames() = throw UnsupportedOperationException()
    override fun returnType() = throw UnsupportedOperationException()
    override fun isStaticInitializer() = throw UnsupportedOperationException()
    override fun location() = throw UnsupportedOperationException()
    override fun compareTo(other: Method?) = throw UnsupportedOperationException()
    override fun isPackagePrivate() = throw UnsupportedOperationException()
    override fun isPrivate() = throw UnsupportedOperationException()
    override fun isProtected() = throw UnsupportedOperationException()
    override fun isPublic() = throw UnsupportedOperationException()
    override fun modifiers() = throw UnsupportedOperationException()
    override fun virtualMachine() = throw UnsupportedOperationException()
}
