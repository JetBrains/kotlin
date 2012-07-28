/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.types.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ref.ClassName;

/**
 * @author Stepan Koltsov
 */
public enum PrimitiveType {

    BOOLEAN("Boolean"),
    CHAR("Char"),
    BYTE("Byte"),
    SHORT("Short"),
    INT("Int"),
    FLOAT("Float"),
    LONG("Long"),
    DOUBLE("Double"),
    ;
    
    private final Name typeName;
    private final Name arrayTypeName;
    private final ClassName className;
    private final ClassName arrayClassName;

    private PrimitiveType(String typeName) {
        this.typeName = Name.identifier(typeName);
        this.arrayTypeName = Name.identifier(typeName + "Array");
        this.className = new ClassName(JetStandardClasses.STANDARD_CLASSES_FQNAME.child(this.typeName), 0);
        this.arrayClassName = new ClassName(JetStandardClasses.STANDARD_CLASSES_FQNAME.child(this.arrayTypeName), 0);
    }

    @NotNull
    public Name getTypeName() {
        return typeName;
    }

    @NotNull
    public Name getArrayTypeName() {
        return arrayTypeName;
    }

    @NotNull
    public ClassName getClassName() {
        return className;
    }

    @NotNull
    public ClassName getArrayClassName() {
        return arrayClassName;
    }
}
