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

package org.jetbrains.kotlin.descriptors;

import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class Visibilities {
    public static final Visibility PRIVATE = new Visibility("private", false) {
        @Override
        public boolean mustCheckInImports() {
            return true;
        }

        @Override
        protected boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            DeclarationDescriptor parent = what;
            while (parent != null) {
                parent = parent.getContainingDeclaration();
                if ((parent instanceof ClassDescriptor && !DescriptorUtils.isDefaultObject(parent)) ||
                    parent instanceof PackageFragmentDescriptor) {
                    break;
                }
            }
            if (parent == null) {
                return false;
            }
            DeclarationDescriptor fromParent = from;
            while (fromParent != null) {
                if (parent == fromParent) {
                    return true;
                }
                if (fromParent instanceof PackageFragmentDescriptor) {
                    return parent instanceof PackageFragmentDescriptor
                           && ((PackageFragmentDescriptor) parent).getFqName().isAncestorOf(((PackageFragmentDescriptor) fromParent).getFqName())
                           && DescriptorUtils.areInSameModule(fromParent, parent);
                }
                fromParent = fromParent.getContainingDeclaration();
            }
            return false;
        }
    };

    public static final Visibility PRIVATE_TO_THIS = new Visibility("private_to_this", false) {
        @Override
        protected boolean isVisible(@NotNull ReceiverValue thisObject, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            if (PRIVATE.isVisible(thisObject, what, from)) {
                DeclarationDescriptor classDescriptor = DescriptorUtils.getParentOfType(what, ClassDescriptor.class);

                if (classDescriptor != null && thisObject instanceof ClassReceiver) {
                    return ((ClassReceiver) thisObject).getDeclarationDescriptor().getOriginal() == classDescriptor.getOriginal();
                }
            }
            return false;
        }

        @Override
        public boolean mustCheckInImports() {
            return true;
        }

        @Override
        public String toString() {
            return "private/*private to this*/";
        }
    };

    public static final Visibility PROTECTED = new Visibility("protected", true) {
        @Override
        public boolean mustCheckInImports() {
            return false;
        }

        @Override
        protected boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            ClassDescriptor classDescriptor = DescriptorUtils.getParentOfType(what, ClassDescriptor.class);
            if (DescriptorUtils.isDefaultObject(classDescriptor)) {
                classDescriptor = DescriptorUtils.getParentOfType(classDescriptor, ClassDescriptor.class);
            }
            if (classDescriptor == null) return false;

            ClassDescriptor fromClass = DescriptorUtils.getParentOfType(from, ClassDescriptor.class, false);
            if (fromClass == null) return false;
            if (DescriptorUtils.isSubclass(fromClass, classDescriptor)) {
                return true;
            }
            return isVisible(receiver, what, fromClass.getContainingDeclaration());
        }
    };

    public static final Visibility INTERNAL = new Visibility("internal", false) {
        @Override
        public boolean mustCheckInImports() {
            return true;
        }

        @Override
        protected boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            //NOTE: supposedly temporarily
            return PUBLIC.isVisible(receiver, what, from);
        }
    };

    public static final Visibility PUBLIC = new Visibility("public", true) {
        @Override
        public boolean mustCheckInImports() {
            return false;
        }

        @Override
        protected boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return true;
        }
    };

    public static final Visibility LOCAL = new Visibility("local", false) {
        @Override
        public boolean mustCheckInImports() {
            throw new IllegalStateException("This method shouldn't be invoked for LOCAL visibility");
        }

        @Override
        protected boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            throw new IllegalStateException("This method shouldn't be invoked for LOCAL visibility");
        }
    };

    public static final Visibility INHERITED = new Visibility("inherited", false) {
        @Override
        public boolean mustCheckInImports() {
            throw new IllegalStateException("This method shouldn't be invoked for INHERITED visibility");
        }

        @Override
        protected boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            throw new IllegalStateException("Visibility is unknown yet"); //This method shouldn't be invoked for INHERITED visibility
        }
    };

    /* Visibility for fake override invisible members (they are created for better error reporting) */
    public static final Visibility INVISIBLE_FAKE = new Visibility("invisible_fake", false) {
        @Override
        public boolean mustCheckInImports() {
            throw new IllegalStateException("This method shouldn't be invoked for INVISIBLE_FAKE visibility");
        }

        @Override
        protected boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return false;
        }
    };

    public static final Set<Visibility> INVISIBLE_FROM_OTHER_MODULES =
            Collections.unmodifiableSet(KotlinPackage.setOf(PRIVATE, PRIVATE_TO_THIS, INTERNAL, LOCAL));

    private Visibilities() {
    }

    public static boolean isVisible(@NotNull ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
        return findInvisibleMember(receiver, what, from) == null;
    }

    @SuppressWarnings("UnusedDeclaration")
    private static boolean isInFriendModule(@NotNull DeclarationDescriptor what, @NotNull DeclarationDescriptor from) {
        return DescriptorUtils.getContainingModule(what).isFriend(DescriptorUtils.getContainingModule(from));
    }

    @Nullable
    public static DeclarationDescriptorWithVisibility findInvisibleMember(
            @NotNull ReceiverValue receiver,
            @NotNull DeclarationDescriptorWithVisibility what,
            @NotNull DeclarationDescriptor from
    ) {
        DeclarationDescriptorWithVisibility parent = what;
        while (parent != null && parent.getVisibility() != LOCAL) {
            if (!parent.getVisibility().isVisible(receiver, parent, from)) {
                return parent;
            }
            parent = DescriptorUtils.getParentOfType(parent, DeclarationDescriptorWithVisibility.class);
        }
        return null;
    }

    private static final Map<Visibility, Integer> ORDERED_VISIBILITIES;

    static {
        Map<Visibility, Integer> visibilities = UtilsPackage.newHashMapWithExpectedSize(4);
        visibilities.put(PRIVATE_TO_THIS, 0);
        visibilities.put(PRIVATE, 0);
        visibilities.put(INTERNAL, 1);
        visibilities.put(PROTECTED, 1);
        visibilities.put(PUBLIC, 2);
        ORDERED_VISIBILITIES = Collections.unmodifiableMap(visibilities);
    }

    /*package*/
    @Nullable
    static Integer compareLocal(@NotNull Visibility first, @NotNull Visibility second) {
        if (first == second) return 0;
        Integer firstIndex = ORDERED_VISIBILITIES.get(first);
        Integer secondIndex = ORDERED_VISIBILITIES.get(second);
        if (firstIndex == null || secondIndex == null || firstIndex.equals(secondIndex)) {
            return null;
        }
        return firstIndex - secondIndex;
    }

    @Nullable
    public static Integer compare(@NotNull Visibility first, @NotNull Visibility second) {
        Integer result = first.compareTo(second);
        if (result != null) {
            return result;
        }
        Integer oppositeResult = second.compareTo(first);
        if (oppositeResult != null) {
            return -oppositeResult;
        }
        return null;
    }

    public static boolean isPrivate(@NotNull Visibility visibility) {
        return visibility == PRIVATE || visibility == PRIVATE_TO_THIS;
    }
}
