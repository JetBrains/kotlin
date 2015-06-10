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

public class FunctionReference
        extends FunctionImpl
        implements KTopLevelFunction,
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

    // The following methods provide the information identifying this function, which is used by the reflection implementation.
    // They are supposed to be overridden in each subclass (each anonymous class generated for a function reference).

    public KDeclarationContainer getOwner() {
        throw error();
    }

    // Kotlin name of the function, the one which was declared in the source code (@platformName can't change it)
    public String getName() {
        throw error();
    }

    // JVM signature of the function, e.g. "println(Ljava/lang/Object;)V"
    public String getSignature() {
        throw error();
    }

    // The following methods are the stub implementations of reflection functions.
    // They are called when you're using reflection on a function reference without the reflection implementation in the classpath.

    // (nothing here yet)

    private static Error error() {
        throw new KotlinReflectionNotSupportedError();
    }
}
