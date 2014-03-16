/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

public abstract class IntegerValueConstant<T> extends CompileTimeConstant<T> {

    /*
    * if false then constant type cannot be changed
    * ex1. val a: Long = 1.toInt() (TYPE_MISMATCH error, 1.toInt() isn't pure)
    * ex2. val b: Int = a (TYPE_MISMATCH error, a isn't pure)
    * */
    private final boolean isPure;

    protected IntegerValueConstant(T value, boolean canBeUsedInAnnotations, boolean pure) {
        super(value, canBeUsedInAnnotations);
        isPure = pure;
    }

    public boolean isPure() {
        return isPure;
    }
}
