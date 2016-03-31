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

package org.jetbrains.kotlin.idea.debugger;

import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;

public class MockLocation implements Location {
    @NotNull private final ReferenceType declaringType;
    @NotNull private final String sourceName;
    private final int lineNumber;

    public MockLocation(@NotNull ReferenceType declaringType, @NotNull String sourceName, int lineNumber) {
        this.declaringType = declaringType;
        this.sourceName = sourceName;
        this.lineNumber = lineNumber;
    }

    @Override
    public ReferenceType declaringType() {
        return declaringType;
    }

    @Override
    public String sourceName() {
        return sourceName;
    }

    @Override
    public int lineNumber() {
        return lineNumber;
    }


    @Override
    public Method method() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long codeIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String sourceName(String s) throws AbsentInformationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String sourcePath() throws AbsentInformationException {
        throw new AbsentInformationException();
    }

    @Override
    public String sourcePath(String s) throws AbsentInformationException {
        throw new AbsentInformationException();
    }

    @Override
    public int lineNumber(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(Location o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualMachine virtualMachine() {
        throw new UnsupportedOperationException();
    }
}
