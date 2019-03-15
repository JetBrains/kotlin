/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger;

import com.sun.jdi.*;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.request.EventRequestManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MockVirtualMachine implements VirtualMachine {
    @Override
    public List<ThreadGroupReference> topLevelThreadGroups() {
        return Collections.emptyList();
    }

    @Override
    public String version() {
        return "1.6";
    }


    @Override
    public List<ReferenceType> classesByName(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ReferenceType> allClasses() {
        // Can't throw since this method is invoked during VirtualMachineProxy initialization
        return Collections.emptyList();
    }

    @Override
    public void redefineClasses(Map<? extends ReferenceType, byte[]> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ThreadReference> allThreads() {
        return Collections.emptyList();
    }

    @Override
    public void suspend() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resume() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EventQueue eventQueue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EventRequestManager eventRequestManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BooleanValue mirrorOf(boolean b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteValue mirrorOf(byte b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CharValue mirrorOf(char c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ShortValue mirrorOf(short i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntegerValue mirrorOf(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LongValue mirrorOf(long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FloatValue mirrorOf(float v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DoubleValue mirrorOf(double v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StringReference mirrorOf(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VoidValue mirrorOfVoid() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Process process() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dispose() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void exit(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefaultStratum(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDefaultStratum() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String description() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String name() {
        return "JVM mock";
    }

    @Override
    public void setDebugTraceMode(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canWatchFieldModification() {
        return false;
    }

    @Override
    public boolean canWatchFieldAccess() {
        return false;
    }

    @Override
    public boolean canGetBytecodes() {
        return false;
    }

    @Override
    public boolean canGetSyntheticAttribute() {
        return false;
    }

    @Override
    public boolean canGetOwnedMonitorInfo() {
        return false;
    }

    @Override
    public boolean canGetCurrentContendedMonitor() {
        return false;
    }

    @Override
    public boolean canGetMonitorInfo() {
        return false;
    }

    @Override
    public boolean canUseInstanceFilters() {
        return false;
    }

    @Override
    public boolean canRedefineClasses() {
        return false;
    }

    @Override
    public boolean canAddMethod() {
        return false;
    }

    @Override
    public boolean canUnrestrictedlyRedefineClasses() {
        return false;
    }

    @Override
    public boolean canPopFrames() {
        return false;
    }

    @Override
    public boolean canGetSourceDebugExtension() {
        return false;
    }

    @Override
    public boolean canRequestVMDeathEvent() {
        return false;
    }

    @Override
    public boolean canGetMethodReturnValues() {
        return false;
    }

    @Override
    public boolean canGetInstanceInfo() {
        return false;
    }

    @Override
    public boolean canUseSourceNameFilters() {
        return false;
    }

    @Override
    public boolean canForceEarlyReturn() {
        return false;
    }

    @Override
    public boolean canBeModified() {
        return false;
    }

    @Override
    public boolean canRequestMonitorEvents() {
        return false;
    }

    @Override
    public boolean canGetMonitorFrameInfo() {
        return false;
    }

    @Override
    public boolean canGetClassFileVersion() {
        return false;
    }

    @Override
    public boolean canGetConstantPool() {
        return false;
    }

    @Override
    public long[] instanceCounts(List<? extends ReferenceType> types) {
        return new long[0];
    }

    @Override
    public VirtualMachine virtualMachine() {
        return this;
    }
}
