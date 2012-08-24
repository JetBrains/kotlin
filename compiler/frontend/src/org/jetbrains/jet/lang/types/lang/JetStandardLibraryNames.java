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
public class JetStandardLibraryNames {

    private JetStandardLibraryNames() {
    }

    @NotNull
    private static ClassName classIn(@NotNull String name, int typeParameterCount) {
        return new ClassName(
                JetStandardClasses.STANDARD_CLASSES_FQNAME.child(Name.identifier(name)),
                typeParameterCount);
    }

    public static final ClassName ANY = classIn("Any", 0);
    public static final ClassName ARRAY = classIn("Array", 1);
    public static final ClassName VOLATILE = classIn("volatile", 0);
    public static final ClassName INT = PrimitiveType.INT.getClassName();
    public static final ClassName COMPARABLE = classIn("Comparable", 1);
    public static final ClassName ITERABLE = classIn("Iterable", 1);
    public static final ClassName ITERATOR = classIn("Iterator", 1);
    public static final ClassName MUTABLE_ITERABLE = classIn("MutableIterable", 1);
    public static final ClassName MUTABLE_ITERATOR = classIn("MutableIterator", 1);
    public static final ClassName NOTHING = classIn("Nothing", 0);
    public static final ClassName STRING = classIn("String", 0);
    public static final ClassName CHAR_SEQUENCE = classIn("CharSequence", 0);
    public static final ClassName NUMBER = classIn("Number", 0);
    public static final ClassName THROWABLE = classIn("Throwable", 0);
    public static final ClassName ENUM = classIn("Enum", 1);
}
