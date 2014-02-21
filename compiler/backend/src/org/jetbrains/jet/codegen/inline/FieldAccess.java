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

package org.jetbrains.jet.codegen.inline;

public class FieldAccess {

    private final String name;

    private final String type;

    private final FieldAccess owner;

    private final boolean isThisAccess;

    public FieldAccess(String name, String type, FieldAccess owner) {
        this.name = name;
        this.type = type;
        this.owner = owner;
        isThisAccess = false;
    }

    public FieldAccess(String name, String type) {
        this.name = "!this!" + name;
        this.type = type;
        isThisAccess = true;
        owner = null;
    }


    public boolean isThisAccess() {
        return isThisAccess;
    }
}
