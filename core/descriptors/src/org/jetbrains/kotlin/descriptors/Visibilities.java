/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import kotlin.collections.SetsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.SuperCallReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver;
import org.jetbrains.kotlin.types.DynamicTypesKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.util.ModuleVisibilityHelper;
import org.jetbrains.kotlin.utils.CollectionsKt;

import java.util.*;

public class Visibilities {
    @NotNull
    public static final Visibility PRIVATE = new Visibility("private", false) {
        @Override
        public boolean mustCheckInImports() {
            return true;
        }

        private boolean hasContainingSourceFile(@NotNull DeclarationDescriptor descriptor) {
            return DescriptorUtils.getContainingSourceFile(descriptor) != SourceFile.NO_SOURCE_FILE;
        }

        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            if (DescriptorUtils.isTopLevelDeclaration(what) && hasContainingSourceFile(from)) {
                return inSameFile(what, from);
            }

            if (what instanceof ConstructorDescriptor) {
                ClassifierDescriptorWithTypeParameters classDescriptor = ((ConstructorDescriptor) what).getContainingDeclaration();
                if (DescriptorUtils.isSealedClass(classDescriptor)
                    && DescriptorUtils.isTopLevelDeclaration(classDescriptor)
                    && from instanceof ConstructorDescriptor
                    && DescriptorUtils.isTopLevelDeclaration(from.getContainingDeclaration())
                    && inSameFile(what, from)) {
                    return true;
                }
            }

            DeclarationDescriptor parent = what;
            while (parent != null) {
                parent = parent.getContainingDeclaration();
                if ((parent instanceof ClassDescriptor && !DescriptorUtils.isCompanionObject(parent)) ||
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
                           && ((PackageFragmentDescriptor) parent).getFqName().equals(((PackageFragmentDescriptor) fromParent).getFqName())
                           && DescriptorUtils.areInSameModule(fromParent, parent);
                }
                fromParent = fromParent.getContainingDeclaration();
            }
            return false;
        }
    };

    /**
     * This visibility is needed for the next case:
     *  class A<in T>(t: T) {
     *      private val t: T = t // visibility for t is PRIVATE_TO_THIS
     *
     *      fun test() {
     *          val x: T = t // correct
     *          val y: T = this.t // also correct
     *      }
     *      fun foo(a: A<String>) {
     *         val x: String = a.t // incorrect, because a.t can be Any
     *      }
     *  }
     */
    @NotNull
    public static final Visibility PRIVATE_TO_THIS = new Visibility("private_to_this", false) {
        @Override
        public boolean isVisible(@Nullable ReceiverValue thisObject, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            if (PRIVATE.isVisible(thisObject, what, from)) {
                // See Visibility.isVisible contract
                if (thisObject == ALWAYS_SUITABLE_RECEIVER) return true;
                if (thisObject == IRRELEVANT_RECEIVER) return false;

                DeclarationDescriptor classDescriptor = DescriptorUtils.getParentOfType(what, ClassDescriptor.class);

                if (classDescriptor != null && thisObject instanceof ThisClassReceiver) {
                    return ((ThisClassReceiver) thisObject).getClassDescriptor().getOriginal().equals(classDescriptor.getOriginal());
                }
            }
            return false;
        }

        @Override
        public boolean mustCheckInImports() {
            return true;
        }

        @NotNull
        @Override
        public String getDisplayName() {
            return "private/*private to this*/";
        }
    };

    @NotNull
    public static final Visibility PROTECTED = new Visibility("protected", true) {
        @Override
        public boolean mustCheckInImports() {
            return false;
        }

        @Override
        public boolean isVisible(
                @Nullable ReceiverValue receiver,
                @NotNull DeclarationDescriptorWithVisibility what,
                @NotNull DeclarationDescriptor from
        ) {
            ClassDescriptor givenDescriptorContainingClass = DescriptorUtils.getParentOfType(what, ClassDescriptor.class);
            ClassDescriptor fromClass = DescriptorUtils.getParentOfType(from, ClassDescriptor.class, false);
            if (fromClass == null) return false;

            if (givenDescriptorContainingClass != null && DescriptorUtils.isCompanionObject(givenDescriptorContainingClass)) {
                // Access to protected members inside companion is allowed to all subclasses
                // Receiver type does not matter because objects are final
                // NB: protected fake overrides in companion from super class should also be allowed
                ClassDescriptor companionOwner = DescriptorUtils.getParentOfType(givenDescriptorContainingClass, ClassDescriptor.class);
                if (companionOwner != null && DescriptorUtils.isSubclass(fromClass, companionOwner)) return true;
            }

            // The rest part of method checks visibility similarly to Java does for protected (see JLS p.6.6.2)

            // Protected fake overrides can have only one protected overridden (as protected is not allowed for interface members)
            DeclarationDescriptorWithVisibility whatDeclaration = DescriptorUtils.unwrapFakeOverrideToAnyDeclaration(what);

            ClassDescriptor classDescriptor = DescriptorUtils.getParentOfType(whatDeclaration, ClassDescriptor.class);
            if (classDescriptor == null) return false;

            if (DescriptorUtils.isSubclass(fromClass, classDescriptor)
                    && doesReceiverFitForProtectedVisibility(receiver, whatDeclaration, fromClass)) {
                return true;
            }

            return isVisible(receiver, what, fromClass.getContainingDeclaration());
        }

        private boolean doesReceiverFitForProtectedVisibility(
                @Nullable ReceiverValue receiver,
                @NotNull DeclarationDescriptorWithVisibility whatDeclaration,
                @NotNull ClassDescriptor fromClass
        ) {
            //noinspection deprecation
            if (receiver == FALSE_IF_PROTECTED) return false;

            // Do not check receiver for non-callable declarations
            if (!(whatDeclaration instanceof CallableMemberDescriptor)) return true;
            // Constructor accessibility check is performed manually
            if (whatDeclaration instanceof ConstructorDescriptor) return true;

            // See Visibility.isVisible contract
            if (receiver == ALWAYS_SUITABLE_RECEIVER) return true;
            if (receiver == IRRELEVANT_RECEIVER || receiver == null) return false;

            KotlinType actualReceiverType = receiver instanceof SuperCallReceiverValue
                                            ? ((SuperCallReceiverValue) receiver).getThisType()
                                            : receiver.getType();

            return DescriptorUtils.isSubtypeOfClass(actualReceiverType, fromClass) || DynamicTypesKt.isDynamic(actualReceiverType);
        }
    };

    @NotNull
    public static final Visibility INTERNAL = new Visibility("internal", false) {
        @Override
        public boolean mustCheckInImports() {
            return true;
        }

        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            DeclarationDescriptor fromOrModule = from instanceof PackageViewDescriptor ? ((PackageViewDescriptor) from).getModule() : from;
            if (!DescriptorUtils.getContainingModule(fromOrModule).shouldSeeInternalsOf(DescriptorUtils.getContainingModule(what))) return false;

            return MODULE_VISIBILITY_HELPER.isInFriendModule(what, from);
        }
    };

    @NotNull
    public static final Visibility PUBLIC = new Visibility("public", true) {
        @Override
        public boolean mustCheckInImports() {
            return false;
        }

        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return true;
        }
    };

    @NotNull
    public static final Visibility LOCAL = new Visibility("local", false) {
        @Override
        public boolean mustCheckInImports() {
            throw new IllegalStateException("This method shouldn't be invoked for LOCAL visibility");
        }

        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            throw new IllegalStateException("This method shouldn't be invoked for LOCAL visibility");
        }
    };

    @NotNull
    public static final Visibility INHERITED = new Visibility("inherited", false) {
        @Override
        public boolean mustCheckInImports() {
            throw new IllegalStateException("This method shouldn't be invoked for INHERITED visibility");
        }

        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            throw new IllegalStateException("Visibility is unknown yet"); //This method shouldn't be invoked for INHERITED visibility
        }
    };

    /* Visibility for fake override invisible members (they are created for better error reporting) */
    @NotNull
    public static final Visibility INVISIBLE_FAKE = new Visibility("invisible_fake", false) {
        @Override
        public boolean mustCheckInImports() {
            throw new IllegalStateException("This method shouldn't be invoked for INVISIBLE_FAKE visibility");
        }

        @Override
        public boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
            return false;
        }
    };

    // Currently used as default visibility of FunctionDescriptor
    // It's needed to prevent NPE when requesting non-nullable visibility of descriptor before `initialize` has been called
    @NotNull
    public static final Visibility UNKNOWN = new Visibility("unknown", false) {
        @Override
        public boolean mustCheckInImports() {
            throw new IllegalStateException("This method shouldn't be invoked for UNKNOWN visibility");
        }

        @Override
        public boolean isVisible(
                @Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from
        ) {
            return false;
        }
    };

    public static final Set<Visibility> INVISIBLE_FROM_OTHER_MODULES =
            Collections.unmodifiableSet(SetsKt.setOf(PRIVATE, PRIVATE_TO_THIS, INTERNAL, LOCAL));

    private Visibilities() {
    }

    public static boolean isVisible(@Nullable ReceiverValue receiver, @NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
        return findInvisibleMember(receiver, what, from) == null;
    }

    /**
     * @see Visibility.isVisible contract
     */
    public static boolean isVisibleIgnoringReceiver(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
        return findInvisibleMember(ALWAYS_SUITABLE_RECEIVER, what, from) == null;
    }

    /**
     * @see Visibility.isVisible contract
     * @see Visibilities.RECEIVER_DOES_NOT_EXIST
     */
    public static boolean isVisibleWithAnyReceiver(@NotNull DeclarationDescriptorWithVisibility what, @NotNull DeclarationDescriptor from) {
        return findInvisibleMember(IRRELEVANT_RECEIVER, what, from) == null;
    }

    // Note that this method returns false if `from` declaration is `init` initializer
    // because initializer does not have source element
    public static boolean inSameFile(@NotNull DeclarationDescriptor what, @NotNull DeclarationDescriptor from) {
        SourceFile fromContainingFile = DescriptorUtils.getContainingSourceFile(from);
        if (fromContainingFile != SourceFile.NO_SOURCE_FILE) {
            return fromContainingFile.equals(DescriptorUtils.getContainingSourceFile(what));
        }
        return false;
    }

    @Nullable
    public static DeclarationDescriptorWithVisibility findInvisibleMember(
            @Nullable ReceiverValue receiver,
            @NotNull DeclarationDescriptorWithVisibility what,
            @NotNull DeclarationDescriptor from
    ) {
        DeclarationDescriptorWithVisibility parent = (DeclarationDescriptorWithVisibility) what.getOriginal();
        while (parent != null && parent.getVisibility() != LOCAL) {
            if (!parent.getVisibility().isVisible(receiver, parent, from)) {
                return parent;
            }
            parent = DescriptorUtils.getParentOfType(parent, DeclarationDescriptorWithVisibility.class);
        }

        if (what instanceof TypeAliasConstructorDescriptor) {
            DeclarationDescriptorWithVisibility invisibleUnderlying =
                    findInvisibleMember(receiver, ((TypeAliasConstructorDescriptor) what).getUnderlyingConstructorDescriptor(), from);
            if (invisibleUnderlying != null) return invisibleUnderlying;
        }

        return null;
    }

    private static final Map<Visibility, Integer> ORDERED_VISIBILITIES;

    static {
        Map<Visibility, Integer> visibilities = CollectionsKt.newHashMapWithExpectedSize(4);
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

    public static final Visibility DEFAULT_VISIBILITY = PUBLIC;

    /**
     * This value should be used for receiverValue parameter of Visibility.isVisible
     * iff there is intention to determine if member is visible for any receiver.
     */
    private static final ReceiverValue IRRELEVANT_RECEIVER = new ReceiverValue() {
        @NotNull
        @Override
        public KotlinType getType() {
            throw new IllegalStateException("This method should not be called");
        }
    };

    /**
     * This value should be used for receiverValue parameter of Visibility.isVisible
     * iff there is intention to determine if member is visible without receiver related checks being performed.
     */
    public static final ReceiverValue ALWAYS_SUITABLE_RECEIVER = new ReceiverValue() {
        @NotNull
        @Override
        public KotlinType getType() {
            throw new IllegalStateException("This method should not be called");
        }
    };

    // This constant is not intended to use somewhere else from
    @Deprecated
    public static final ReceiverValue FALSE_IF_PROTECTED = new ReceiverValue() {
        @NotNull
        @Override
        public KotlinType getType() {
            throw new IllegalStateException("This method should not be called");
        }
    };

    public static boolean isPrivate(@NotNull Visibility visibility) {
        return visibility == PRIVATE || visibility == PRIVATE_TO_THIS;
    }

    @NotNull
    private static final ModuleVisibilityHelper MODULE_VISIBILITY_HELPER;

    static {
        Iterator<ModuleVisibilityHelper> iterator = ServiceLoader.load(ModuleVisibilityHelper.class, ModuleVisibilityHelper.class.getClassLoader()).iterator();
        MODULE_VISIBILITY_HELPER = iterator.hasNext() ? iterator.next() : ModuleVisibilityHelper.EMPTY.INSTANCE;
    }
}
