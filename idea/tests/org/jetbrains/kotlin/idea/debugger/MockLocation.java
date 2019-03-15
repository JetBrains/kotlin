/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
        return new MockMethod();
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
