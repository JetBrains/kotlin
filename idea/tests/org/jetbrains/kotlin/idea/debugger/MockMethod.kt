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

package org.jetbrains.kotlin.idea.debugger

import com.sun.jdi.Method

class MockMethod : Method {
    override fun name() = ""

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
    override fun allLineLocations() = throw UnsupportedOperationException()
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
