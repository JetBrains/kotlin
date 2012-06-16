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

package org.jetbrains.jet.lang.resolve.name;

import com.google.common.base.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

/**
 * @author Stepan Koltsov
 */
public abstract class NamePredicate {

    public boolean isAll() {
        return this instanceof All;
    }

    @Nullable
    public Name getExact() {
        if (this instanceof Exact) {
            return ((Exact) this).required;
        }
        else {
            return null;
        }
    }


    public abstract boolean matches(@NotNull Name name);


    private static class All extends NamePredicate {
        public static final All instance = new All();

        @Override
        public boolean matches(@NotNull Name name) {
            return true;
        }
    }

    public static All all() {
        return new All();
    }


    private static class Exact extends NamePredicate {
        @NotNull
        private final Name required;

        private Exact(@NotNull Name required) {
            this.required = required;
        }

        @Override
        public boolean matches(@NotNull Name name) {
            return name.equals(required);
        }
    }

    public static Exact exact(@NotNull Name required) {
        return new Exact(required);
    }


    public Predicate<DeclarationDescriptor> asGuavaDescriptorPredicate() {
        return new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(@NotNull DeclarationDescriptor descriptor) {
                return matches(descriptor.getName());
            }
        };
    }

}
