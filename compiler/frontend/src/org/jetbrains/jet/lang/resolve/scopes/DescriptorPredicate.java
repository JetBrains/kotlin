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

package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.base.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.name.NamePredicate;

/**
 * @author Stepan Koltsov
 */
public abstract class DescriptorPredicate {

    public boolean includeAll() {
        return this instanceof All;
    }

    public abstract boolean includeName(@NotNull Name name);

    public boolean includeAnyName() {
        return includeAll();
    }

    public enum DescriptorKind {
        CLASS,
        CALLABLE_MEMBER,
        CALLABLE_NON_MEMBER,
        NAMESPACE,
        OTHER,
        ;

        public boolean isCallable() {
            return this == CALLABLE_MEMBER || this == CALLABLE_NON_MEMBER;
        }
    }

    public abstract boolean includeKind(@NotNull DescriptorKind kind);

    private static DescriptorKind descriptorKind(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            return DescriptorKind.CALLABLE_MEMBER;
        }
        else if (descriptor instanceof CallableDescriptor) {
            return DescriptorKind.CALLABLE_NON_MEMBER;
        }
        else if (descriptor instanceof ClassDescriptor) {
            return DescriptorKind.CLASS;
        }
        else if (descriptor instanceof NamespaceDescriptor) {
            return DescriptorKind.NAMESPACE;
        }
        else {
            return DescriptorKind.OTHER;
        }
    }

    /**
     * Should be called only if includeKind(CALLABLE_MEMBER) returned true.
     */
    public abstract boolean includeExtension(boolean extension);


    public boolean include(@NotNull DeclarationDescriptor descriptor) {
        if (!includeKind(descriptorKind(descriptor))) {
            return false;
        }

        if (descriptor instanceof CallableMemberDescriptor) {
            CallableMemberDescriptor callable = (CallableMemberDescriptor) descriptor;
            boolean extension = callable.getReceiverParameter().exists();
            if (!includeExtension(extension)) {
                return false;
            }
        }

        if (!includeName(descriptor.getName())) {
            return false;
        }

        return true;
    }


    private static class All extends DescriptorPredicate {
        private static final All instance = new All();

        @Override
        public boolean include(@NotNull DeclarationDescriptor descriptor) {
            return true;
        }

        @Override
        public boolean includeKind(@NotNull DescriptorKind kind) {
            return true;
        }

        @Override
        public boolean includeName(@NotNull Name name) {
            return true;
        }

        @Override
        public boolean includeExtension(boolean extension) {
            return true;
        }
    }

    public static DescriptorPredicate all() {
        return All.instance;
    }


    private static class HasName extends DescriptorPredicate {
        @NotNull
        private final NamePredicate required;

        private HasName(@NotNull NamePredicate required) {
            this.required = required;
        }

        @Override
        public boolean includeKind(@NotNull DescriptorKind kind) {
            return true;
        }

        @Override
        public boolean includeExtension(boolean extension) {
            return true;
        }

        @Override
        public boolean include(@NotNull DeclarationDescriptor descriptor) {
            return includeName(descriptor.getName());
        }

        @Override
        public boolean includeName(@NotNull Name name) {
            return required.matches(name);
        }

    }

    public static DescriptorPredicate hasName(@NotNull NamePredicate required) {
        return new HasName(required);
    }


    private static class MembersOfType extends DescriptorPredicate {

        @NotNull
        private final DescriptorKind required;

        private MembersOfType(@NotNull DescriptorKind required) {
            this.required = required;
        }

        @Override
        public boolean includeName(@NotNull Name name) {
            return true;
        }

        @Override
        public boolean includeAnyName() {
            return true;
        }

        @Override
        public boolean includeExtension(boolean extension) {
            return true;
        }

        @Override
        public boolean include(@NotNull DeclarationDescriptor descriptor) {
            return descriptorKind(descriptor) == required;
        }

        @Override
        public boolean includeKind(@NotNull DescriptorKind kind) {
            return kind == required;
        }

    }


    private static final DescriptorPredicate callableMembers = new MembersOfType(DescriptorKind.CALLABLE_MEMBER);

    public static DescriptorPredicate callableMembers() {
        return callableMembers;
    }

    private static final DescriptorPredicate namespaces = new MembersOfType(DescriptorKind.NAMESPACE);

    public static DescriptorPredicate namespaces() {
        return namespaces;
    }


    private static class MembersOrTypeWithName extends MembersOfType {
        @NotNull
        private final NamePredicate required;

        private MembersOrTypeWithName(@NotNull DescriptorKind descriptorKind, @NotNull NamePredicate required) {
            super(descriptorKind);
            this.required = required;
        }

        @Override
        public boolean includeName(@NotNull Name name) {
            return required.matches(name);
        }

        @Override
        public boolean includeAnyName() {
            return false;
        }
    }

    public static DescriptorPredicate callableMembers(@NotNull NamePredicate required) {
        if (required.isAll()) {
            return callableMembers();
        }
        else {
            return new MembersOrTypeWithName(DescriptorKind.CALLABLE_MEMBER, required);
        }
    }

    public static DescriptorPredicate namespaces(@NotNull NamePredicate required) {
        if (required.isAll()) {
            return namespaces();
        }
        else {
            return new MembersOrTypeWithName(DescriptorKind.NAMESPACE, required);
        }
    }


    private static class Extension extends MembersOfType {
        private static final Extension instance = new Extension();

        private Extension() {
            super(DescriptorKind.CALLABLE_MEMBER);
        }

        @Override
        public boolean includeExtension(boolean extension) {
            return extension;
        }
    }

    public static DescriptorPredicate extension() {
        return Extension.instance;
    }



    private static class ExtensionWithName extends Extension {
        @NotNull
        private final NamePredicate required;

        private ExtensionWithName(@NotNull NamePredicate required) {
            this.required = required;
        }

        @Override
        public boolean includeName(@NotNull Name name) {
            return required.matches(name);
        }
    }

    public static DescriptorPredicate extension(@NotNull NamePredicate required) {
        if (required.isAll()) {
            return extension();
        }
        else {
            return new ExtensionWithName(required);
        }
    }


    public Predicate<DeclarationDescriptor> asGuavaPredicate() {
        return new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(@NotNull DeclarationDescriptor descriptor) {
                return include(descriptor);
            }
        };
    }

    public NamePredicate asNamePredicate() {
        if (includeAnyName()) {
            return NamePredicate.all();
        }
        else {
            return new NamePredicate() {
                @Override
                public boolean matches(@NotNull Name name) {
                    return includeName(name);
                }
            };
        }
    }

}
