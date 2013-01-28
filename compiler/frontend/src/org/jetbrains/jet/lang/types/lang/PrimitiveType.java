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

package org.jetbrains.jet.lang.types.lang;

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

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

    public static final ImmutableSet<PrimitiveType> NUMBER_TYPES = ImmutableSet.of(CHAR, BYTE, SHORT, INT, FLOAT, LONG, DOUBLE);
    
    private final Name typeName;
    private final Name arrayTypeName;
    private final Name rangeTypeName;
    private final FqName className;
    private final FqName arrayClassName;
    private final FqName rangeClassName;

    private PrimitiveType(String typeName) {
        this.typeName = Name.identifier(typeName);
        this.arrayTypeName = Name.identifier(typeName + "Array");
        this.rangeTypeName = Name.identifier(typeName + "Range");
        FqName builtInsPackageFqName = KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME;
        this.className = builtInsPackageFqName.child(this.typeName);
        this.arrayClassName = builtInsPackageFqName.child(this.arrayTypeName);
        this.rangeClassName = builtInsPackageFqName.child(this.rangeTypeName);
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
    public Name getRangeTypeName() {
        return rangeTypeName;
    }

    @NotNull
    public FqName getClassName() {
        return className;
    }

    @NotNull
    public FqName getArrayClassName() {
        return arrayClassName;
    }

    @NotNull
    public FqName getRangeClassName() {
        return rangeClassName;
    }
}
