/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.mock

import com.sun.jdi.*

class MockLocation(private val declaringType: ReferenceType, private val sourceName: String, private val lineNumber: Int) : Location {
    override fun declaringType() = declaringType
    override fun sourceName() = sourceName
    override fun lineNumber() = lineNumber
    override fun method() = MockMethod()
    override fun codeIndex() = throw UnsupportedOperationException()
    override fun sourceName(s: String) = throw UnsupportedOperationException()
    override fun sourcePath() = throw AbsentInformationException()
    override fun sourcePath(s: String) = throw AbsentInformationException()
    override fun lineNumber(s: String) = throw UnsupportedOperationException()
    override fun compareTo(other: Location) = throw UnsupportedOperationException()
    override fun virtualMachine() = throw UnsupportedOperationException()
}
