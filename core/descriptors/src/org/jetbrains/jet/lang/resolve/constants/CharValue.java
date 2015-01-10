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

package org.jetbrains.jet.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.kotlin.types.JetType;

public class CharValue extends IntegerValueConstant<Character> {

    public CharValue(char value, boolean canBeUsedInAnnotations, boolean pure, boolean usesVariableAsConstant) {
        super(value, canBeUsedInAnnotations, pure, usesVariableAsConstant);
    }

    @NotNull
    @Override
    public JetType getType(@NotNull KotlinBuiltIns kotlinBuiltIns) {
        return kotlinBuiltIns.getCharType();
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitCharValue(this, data);
    }

    @Override
    public String toString() {
        return String.format("\\u%04X ('%s')", (int) value, getPrintablePart(value));
    }

    private static String getPrintablePart(char c) {
        switch (c) {
            case '\b':
                return "\\b";
            case '\t':
                return "\\t";
            case '\n':
                return "\\n";
            case '\f':
                return "\\f";
            case '\r':
                return "\\r";
            default:
                return isPrintableUnicode(c) ? Character.toString(c) : "?";
        }
    }

    private static boolean isPrintableUnicode(char c) {
        int t = Character.getType(c);
        return t != Character.UNASSIGNED && t != Character.LINE_SEPARATOR && t != Character.PARAGRAPH_SEPARATOR &&
               t != Character.CONTROL && t != Character.FORMAT && t != Character.PRIVATE_USE && t != Character.SURROGATE;
    }
}
