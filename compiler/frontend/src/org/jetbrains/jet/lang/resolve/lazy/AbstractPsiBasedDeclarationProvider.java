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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils.safeNameForLazyResolve;

public abstract class AbstractPsiBasedDeclarationProvider implements DeclarationProvider {
    private final List<JetDeclaration> allDeclarations = Lists.newArrayList();
    private final Multimap<Name, JetNamedFunction> functions = HashMultimap.create();
    private final Multimap<Name, JetProperty> properties = HashMultimap.create();
    private final Multimap<Name, JetClassOrObject> classesAndObjects = ArrayListMultimap.create(); // order matters here

    private boolean indexCreated = false;

    protected final void createIndex() {
        if (indexCreated) return;
        indexCreated = true;

        doCreateIndex();
    }

    protected abstract void doCreateIndex();

    protected void putToIndex(JetDeclaration declaration) {
        if (declaration instanceof JetClassInitializer) {
            return;
        }
        allDeclarations.add(declaration);
        if (declaration instanceof JetNamedFunction) {
            JetNamedFunction namedFunction = (JetNamedFunction) declaration;
            functions.put(safeNameForLazyResolve(namedFunction), namedFunction);
        }
        else if (declaration instanceof JetProperty) {
            JetProperty property = (JetProperty) declaration;
            properties.put(safeNameForLazyResolve(property), property);
        }
        else if (declaration instanceof JetClassOrObject) {
            JetClassOrObject classOrObject = (JetClassOrObject) declaration;
            classesAndObjects.put(safeNameForLazyResolve(classOrObject.getNameAsName()), classOrObject);
        }
        else if (declaration instanceof JetParameter || declaration instanceof JetTypedef || declaration instanceof JetMultiDeclaration) {
            // Do nothing, just put it into allDeclarations is enough
        }
        else {
            throw new IllegalArgumentException("Unknown declaration: " + declaration);
        }
    }

    @Override
    public List<JetDeclaration> getAllDeclarations() {
        createIndex();
        return allDeclarations;
    }

    @NotNull
    @Override
    public List<JetNamedFunction> getFunctionDeclarations(@NotNull Name name) {
        createIndex();
        return Lists.newArrayList(functions.get(name));
    }

    @NotNull
    @Override
    public List<JetProperty> getPropertyDeclarations(@NotNull Name name) {
        createIndex();
        return Lists.newArrayList(properties.get(name));
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> getClassOrObjectDeclarations(@NotNull Name name) {
        createIndex();
        return classesAndObjects.get(name);
    }
}
