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

import kotlin.reflect.KCallable;
import kotlin.reflect.KFunction;

public class FunctionReference extends CallableReference implements FunctionImpl, KFunction {
    private final int arity;

    public FunctionReference(int arity) {
        this.arity = arity;
    }

    @Override
    public int getArity() {
        return arity;
    }

    @Override
    protected KFunction getReflected() {
        return (KFunction) super.getReflected();
    }

    @Override
    protected KCallable computeReflected() {
        return Reflection.function(this);
    }

    @Override
    public boolean isInline() {
        return getReflected().isInline();
    }

    @Override
    public boolean isExternal() {
        return getReflected().isExternal();
    }

    @Override
    public boolean isOperator() {
        return getReflected().isOperator();
    }

    @Override
    public boolean isInfix() {
        return getReflected().isInfix();
    }

    @Override
    public boolean isTailrec() {
        return getReflected().isTailrec();
    }

    @Override
    public boolean isSuspend() {
        return getReflected().isSuspend();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof FunctionReference) {
            FunctionReference other = (FunctionReference) obj;
            return getOwner().equals(other.getOwner()) &&
                   getName().equals(other.getName()) &&
                   getSignature().equals(other.getSignature());
        }
        if (obj instanceof KFunction) {
            return obj.equals(compute());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (getOwner().hashCode() * 31 + getName().hashCode()) * 31 + getSignature().hashCode();
    }

    @Override
    public String toString() {
        KCallable reflected = compute();
        if (reflected != this) {
            return reflected.toString();
        }

        // TODO: consider adding the class name to toString() for constructors
        return "<init>".equals(getName())
               ? "constructor" + Reflection.REFLECTION_NOT_AVAILABLE
               : "function " + getName() + Reflection.REFLECTION_NOT_AVAILABLE;
    }
}
