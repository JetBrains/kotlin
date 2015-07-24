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

package kotlin.jvm.internal;

import kotlin.jvm.KotlinReflectionNotSupportedError;
import kotlin.reflect.*;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.List;

@SuppressWarnings("deprecation")
public class FunctionReference
        extends FunctionImpl
        implements KFunction,
                   KTopLevelFunction,
                   KMemberFunction,
                   KTopLevelExtensionFunction,
                   KLocalFunction {
    private final int arity;

    public FunctionReference(int arity) {
        this.arity = arity;
    }

    @Deprecated // preserved for binary compatibility with M12 release
    public FunctionReference() {
        this.arity = 0;
    }

    @Override
    public int getArity() {
        return arity;
    }

    // Most of the following methods are copies from CallableReference, since this class cannot inherit from it

    public KDeclarationContainer getOwner() {
        throw error();
    }

    @Override
    public String getName() {
        throw error();
    }

    public String getSignature() {
        throw error();
    }

    @Override
    public List<KParameter> getParameters() {
        throw error();
    }

    @Override
    public KType getReturnType() {
        throw error();
    }

    @Override
    public List<Annotation> getAnnotations() {
        throw error();
    }

    @Override
    public Object call(@NotNull Object... args) {
        throw error();
    }

    protected static Error error() {
        throw new KotlinReflectionNotSupportedError();
    }
}
