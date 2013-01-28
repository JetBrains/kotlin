/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

public class OwnerKind {
    private final String name;

    protected OwnerKind(String name) {
        this.name = name;
    }

    public static final OwnerKind NAMESPACE = new OwnerKind("namespace");
    public static final OwnerKind IMPLEMENTATION = new OwnerKind("implementation");
    public static final OwnerKind TRAIT_IMPL = new OwnerKind("trait implementation");

    public static class StaticDelegateKind extends OwnerKind {
        private final String ownerClass;

        public StaticDelegateKind(String ownerClass) {
            super("staticDelegateKind");
            this.ownerClass = ownerClass;
        }

        public String getOwnerClass() {
            return ownerClass;
        }
    }

    @Override
    public String toString() {
        return "OwnerKind(" + name + ")";
    }
}
