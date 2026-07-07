/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi;

import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A minimal interface that {@link KtClassOrObject} implements for the purpose of code-generation that does not need the full power of PSI.
 * This interface can be easily implemented by synthetic elements to generate code for them.
 */
public interface KtPureClassOrObject extends KtPureElement, KtDeclarationContainer {
    /**
     * Returns the simple name of this class or object, or {@code null} if it has none (for example, an object literal).
     */
    @Nullable
    String getName();

    /**
     * Returns {@code true} if this class or object is declared in a local scope (inside a function body or another
     * block) rather than as a top-level or member declaration.
     */
    boolean isLocal();

    /**
     * Returns the supertype list entries (the types and delegations written after the {@code :}), or an empty list if
     * there are none.
     */
    @NotNull
    @ReadOnly
    List<KtSuperTypeListEntry> getSuperTypeListEntries();

    /**
     * Returns the companion objects declared in this class or object body, or an empty list if there are none.
     */
    @NotNull
    @ReadOnly
    List<KtObjectDeclaration> getCompanionObjects();

    /**
     * Returns {@code true} if this class or object has an explicitly written primary constructor.
     */
    boolean hasExplicitPrimaryConstructor();

    /**
     * Returns {@code true} if this class or object has a primary constructor, whether written explicitly or implied by
     * the absence of secondary constructors.
     */
    boolean hasPrimaryConstructor();

    /**
     * Returns the primary constructor, or {@code null} if there is no explicitly written primary constructor.
     */
    @Nullable
    KtPrimaryConstructor getPrimaryConstructor();

    /**
     * Returns the modifier list of the primary constructor (for example, its visibility modifiers), or {@code null} if
     * there is no explicitly written primary constructor or it has no modifiers.
     */
    @Nullable
    KtModifierList getPrimaryConstructorModifierList();

    /**
     * Returns the value parameters of the primary constructor, or an empty list if there are none.
     */
    @NotNull
    @ReadOnly
    List<KtParameter> getPrimaryConstructorParameters();

    /**
     * Returns the secondary constructors declared in this class or object body, or an empty list if there are none.
     */
    @NotNull
    @ReadOnly
    List<KtSecondaryConstructor> getSecondaryConstructors();

    /**
     * Returns the context receivers declared on this class or object, or an empty list if there are none.
     */
    @NotNull
    @ReadOnly
    List<KtContextReceiver> getContextReceivers();

    /**
     * Returns the body of this class or object (the part enclosed in braces), or {@code null} if it has no body.
     */
    @Nullable
    KtClassBody getBody();
}

