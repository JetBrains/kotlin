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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MockReferenceType implements ReferenceType {
    private final String name;

    public MockReferenceType(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }


    @Override
    public String signature() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String genericSignature() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassLoaderReference classLoader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String sourceName() throws AbsentInformationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> sourceNames(String s) throws AbsentInformationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> sourcePaths(String s) throws AbsentInformationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String sourceDebugExtension() throws AbsentInformationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStatic() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAbstract() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isFinal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPrepared() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVerified() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInitialized() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean failedToInitialize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Field> fields() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Field> visibleFields() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Field> allFields() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Field fieldByName(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Method> methods() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Method> visibleMethods() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Method> allMethods() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Method> methodsByName(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Method> methodsByName(String s, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ReferenceType> nestedTypes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value getValue(Field field) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Field, Value> getValues(List<? extends Field> fields) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassObjectReference classObject() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Location> allLineLocations() throws AbsentInformationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Location> allLineLocations(String s, String s1) throws AbsentInformationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Location> locationsOfLine(int i) throws AbsentInformationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Location> locationsOfLine(String s, String s1, int i) throws AbsentInformationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> availableStrata() {
        return Collections.emptyList();
    }

    @Override
    public String defaultStratum() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ObjectReference> instances(long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int majorVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int minorVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int constantPoolCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] constantPool() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int modifiers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPrivate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPackagePrivate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProtected() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPublic() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(ReferenceType o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualMachine virtualMachine() {
        throw new UnsupportedOperationException();
    }
}
